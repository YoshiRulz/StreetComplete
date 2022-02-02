package de.westnordost.streetcomplete.quests.parking_fee

import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.osm.osmquests.OsmFilterQuestType
import de.westnordost.streetcomplete.data.osm.osmquests.Tags
import de.westnordost.streetcomplete.data.user.achievements.QuestTypeAchievement.CAR

class AddParkingFee : OsmFilterQuestType<Fee>() {

    override val elementFilter = """
        nodes, ways, relations with amenity = parking
        and access ~ yes|customers|public
        and (
            !fee and !fee:conditional
            or fee older today -8 years
        )
    """
    override val changesetComment = "Add whether there is a parking fee"
    override val wikiLink = "Tag:amenity=parking"
    override val icon = R.drawable.ic_quest_parking_fee

    override val questTypeAchievements = listOf(CAR)

    override fun getTitle(tags: Map<String, String>) = R.string.quest_parking_fee_title

    override fun createForm() = AddParkingFeeForm()

    override fun applyAnswerTo(answer: Fee, tags: Tags, timestampEdited: Long) =
        answer.applyTo(tags)
}
