package de.westnordost.streetcomplete.overlays.address

import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.core.view.isGone
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.meta.AbbreviationsByLocale
import de.westnordost.streetcomplete.data.osm.edits.create.CreateNodeAction
import de.westnordost.streetcomplete.data.osm.edits.update_tags.StringMapChangesBuilder
import de.westnordost.streetcomplete.data.osm.edits.update_tags.UpdateElementTagsAction
import de.westnordost.streetcomplete.data.osm.mapdata.LatLon
import de.westnordost.streetcomplete.databinding.FragmentOverlayAddressBinding
import de.westnordost.streetcomplete.osm.address.AddressNumber
import de.westnordost.streetcomplete.osm.address.AddressNumberAndNameInputViewController
import de.westnordost.streetcomplete.osm.address.HouseAndBlockNumber
import de.westnordost.streetcomplete.osm.address.PlaceName
import de.westnordost.streetcomplete.osm.address.StreetName
import de.westnordost.streetcomplete.osm.address.StreetOrPlaceName
import de.westnordost.streetcomplete.osm.address.StreetOrPlaceNameViewController
import de.westnordost.streetcomplete.osm.address.applyTo
import de.westnordost.streetcomplete.osm.address.createAddressNumber
import de.westnordost.streetcomplete.osm.address.streetHouseNumber
import de.westnordost.streetcomplete.overlays.AbstractOverlayForm
import de.westnordost.streetcomplete.quests.AnswerItem
import de.westnordost.streetcomplete.quests.road_name.RoadNameSuggestionsSource
import de.westnordost.streetcomplete.util.getNameAndLocationLabel
import org.koin.android.ext.android.inject

class AddressOverlayForm : AbstractOverlayForm() {

    override val contentLayoutResId = R.layout.fragment_overlay_address
    private val binding by contentViewBinding(FragmentOverlayAddressBinding::bind)

    private val abbreviationsByLocale: AbbreviationsByLocale by inject()
    private val roadNameSuggestionsSource: RoadNameSuggestionsSource by inject()

    private lateinit var numberOrNameInputCtrl: AddressNumberAndNameInputViewController
    private lateinit var streetOrPlaceCtrl: StreetOrPlaceNameViewController

    private var addressNumber: AddressNumber? = null
    private var houseName: String? = null
    private var streetOrPlaceName: StreetOrPlaceName? = null

    private var isShowingHouseName: Boolean = false
    private var isShowingPlaceName = false

    override val otherAnswers = listOf(
        AnswerItem(R.string.quest_address_answer_house_name2) { showHouseName() },
        AnswerItem(R.string.quest_address_street_no_named_streets) { showPlaceName() }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addressNumber = element?.tags?.let { createAddressNumber(it) }
        houseName = element?.tags?.get("addr:housename")
        val placeName = element?.tags?.get("addr:place")
        val streetName = element?.tags?.get("addr:street")
        streetOrPlaceName = streetName?.let { StreetName(it) } ?: placeName?.let { PlaceName(it) }

        isShowingPlaceName = savedInstanceState?.getBoolean(SHOW_PLACE_NAME) ?: (placeName != null)
        isShowingHouseName = savedInstanceState?.getBoolean(SHOW_HOUSE_NAME) ?: (houseName != null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tags = element?.tags
        if (tags != null) {
            setTitleHintLabel(getNameAndLocationLabel(
                tags, resources, featureDictionary,
                showHouseNumber = false
            ))
        }
        setMarkerIcon(R.drawable.ic_quest_housenumber)

        val streetOrPlaceBinding = binding.streetOrPlaceNameContainer
        streetOrPlaceCtrl = StreetOrPlaceNameViewController(
            select = streetOrPlaceBinding.streetOrPlaceSelect,
            placeNameInputContainer = streetOrPlaceBinding.placeNameInputContainer,
            placeNameInput = streetOrPlaceBinding.placeNameInput.apply { hint = lastPlaceName },
            streetNameInputContainer = streetOrPlaceBinding.streetNameInputContainer,
            streetNameInput = streetOrPlaceBinding.streetNameInput.apply { hint = lastStreetName },
            roadNameSuggestionsSource = roadNameSuggestionsSource,
            abbreviationsByLocale = abbreviationsByLocale,
            countryLocale = countryInfo.locale
        )
        streetOrPlaceCtrl.streetOrPlaceName = streetOrPlaceName
        streetOrPlaceCtrl.onInputChanged = { checkIsFormComplete() }

        // initially do not show the select for place name
        if (!isShowingPlaceName) {
            streetOrPlaceBinding.streetOrPlaceSelect.isGone = true
        }

        val numberOrNameBinding = binding.addressNumberOrNameContainer
        val numberView = layoutInflater.inflate(
            getAddressNumberLayoutResId(countryInfo.countryCode),
            numberOrNameBinding.countrySpecificContainer
        )
        numberOrNameInputCtrl = AddressNumberAndNameInputViewController(
            toggleHouseNameButton = numberOrNameBinding.toggleHouseNameButton,
            houseNameInput = numberOrNameBinding.houseNameInput,
            toggleAddressNumberButton = numberOrNameBinding.toggleAddressNumberButton,
            addressNumberContainer = numberOrNameBinding.addressNumberContainer,
            activity = requireActivity(),
            houseNumberInput = numberView.findViewById<EditText?>(R.id.houseNumberInput)?.apply { hint = lastHouseNumber },
            blockNumberInput = numberView.findViewById<EditText?>(R.id.blockNumberInput)?.apply { hint = lastBlockNumber },
            conscriptionNumberInput = numberView.findViewById(R.id.conscriptionNumberInput),
            streetNumberInput = numberView.findViewById(R.id.streetNumberInput),
            toggleKeyboardButton = numberOrNameBinding.toggleKeyboardButton,
            addButton = numberView.findViewById(R.id.addButton),
            subtractButton = numberView.findViewById(R.id.subtractButton),
        )
        numberOrNameInputCtrl.addressNumber = addressNumber
        numberOrNameInputCtrl.houseName = houseName
        numberOrNameInputCtrl.onInputChanged = {
            streetOrPlaceCtrl.applyHint()
            checkIsFormComplete()
        }

        // initially do not show any house number / house name UI
        if (!isShowingHouseName) {
            numberOrNameBinding.toggleAddressNumberButton.isGone = true
            numberOrNameBinding.toggleHouseNameButton.isGone = true
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(SHOW_PLACE_NAME, isShowingPlaceName)
        outState.putBoolean(SHOW_HOUSE_NAME, isShowingHouseName)
    }

    override fun onClickMapAt(position: LatLon, clickAreaSizeInMeters: Double): Boolean {
        return streetOrPlaceCtrl.selectStreetAt(position, clickAreaSizeInMeters)
    }

    override fun hasChanges(): Boolean =
        numberOrNameInputCtrl.addressNumber != addressNumber
        || numberOrNameInputCtrl.houseName != houseName
        || streetOrPlaceCtrl.streetOrPlaceName != streetOrPlaceName

    override fun isFormComplete(): Boolean =
        numberOrNameInputCtrl.isComplete && streetOrPlaceCtrl.streetOrPlaceName != null
        // can also be empty to delete the address tagging
        || numberOrNameInputCtrl.isEmpty && streetOrPlaceCtrl.streetOrPlaceName == null

    override fun onClickOk() {
        val number = numberOrNameInputCtrl.addressNumber
        val houseName = numberOrNameInputCtrl.houseName
        val streetOrPlaceName = streetOrPlaceCtrl.streetOrPlaceName

        if (number is HouseAndBlockNumber) { number.blockNumber.let { lastBlockNumber = it } }
        number?.streetHouseNumber?.let { lastHouseNumber = it }
        lastPlaceName = if (streetOrPlaceName is PlaceName) streetOrPlaceName.name else null
        lastStreetName = if (streetOrPlaceName is StreetName) streetOrPlaceName.name else null

        val tagChanges = StringMapChangesBuilder(element?.tags ?: emptyMap())

        number?.applyTo(tagChanges)
        houseName?.let { tagChanges["addr:housename"] = it }
        streetOrPlaceName?.applyTo(tagChanges)

        if (element != null) {
            applyEdit(UpdateElementTagsAction(tagChanges.create()))
        } else {
            applyEdit(CreateNodeAction(geometry.center, tagChanges))
        }
    }

    /* ------------------------------ Show house name / place name ------------------------------ */

    private fun showHouseName() {
        isShowingHouseName = true
        binding.addressNumberOrNameContainer.toggleAddressNumberButton.isGone = false
        binding.addressNumberOrNameContainer.toggleHouseNameButton.isGone = false
        numberOrNameInputCtrl.setHouseNameViewExpanded(true)
        binding.addressNumberOrNameContainer.houseNameInput.requestFocus()
    }

    private fun showPlaceName() {
        isShowingPlaceName = true
        binding.streetOrPlaceNameContainer.streetOrPlaceSelect.isGone = false
        streetOrPlaceCtrl.selectPlaceName()
    }

    companion object {
        private var lastBlockNumber: String? = null
        private var lastHouseNumber: String? = null
        private var lastPlaceName: String? = null
        private var lastStreetName: String? = null

        private const val SHOW_PLACE_NAME = "show_place_name"
        private const val SHOW_HOUSE_NAME = "show_house_name"
    }
}

private fun getAddressNumberLayoutResId(countryCode: String): Int = when (countryCode) {
    "JP" -> R.layout.view_house_number_japan
    "CZ" -> R.layout.view_house_number_czechia
    "SK" -> R.layout.view_house_number_slovakia
    else -> R.layout.view_house_number
}
