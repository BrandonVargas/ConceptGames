package dev.brandonvargas.conceptgames.engine.games.builders

import dev.brandonvargas.conceptgames.engine.games.BuildSettings
import dev.brandonvargas.conceptgames.engine.games.GameBuilder
import dev.brandonvargas.conceptgames.engine.games.GameId
import dev.brandonvargas.conceptgames.engine.games.GameRound
import dev.brandonvargas.conceptgames.engine.model.StudyPack
import dev.brandonvargas.conceptgames.engine.model.TimelineStep

/** Drag the steps or events back into order. */
data class TimelineRound(
    val title: String,
    /** Steps in the correct order. */
    val steps: List<TimelineStep>,
    /** The order shown at the start, as indexes into [steps]. Never already solved. */
    val startOrder: List<Int>,
) : GameRound {
    override val gameId = GameId.Timeline
}

object TimelineBuilder : GameBuilder<TimelineRound> {
    override val id = GameId.Timeline

    override fun isAvailable(pack: StudyPack) = pack.timelines.isNotEmpty()

    override fun build(pack: StudyPack, settings: BuildSettings): TimelineRound {
        val random = settings.random
        val timeline = pack.timelines.random(random)
        val max = settings.byDifficulty(5, 6, 8)
        // Keep a random subset in its original relative order.
        val steps = timeline.steps.indices.shuffled(random).take(max).sorted().map(timeline.steps::get)
        var order = steps.indices.shuffled(random)
        while (order == steps.indices.toList()) order = steps.indices.shuffled(random)
        return TimelineRound(timeline.title, steps, order)
    }
}

/** Drop each item into the bucket it belongs to. */
data class CategorizeRound(val categories: List<String>, val items: List<Item>) : GameRound {
    override val gameId = GameId.Categorize

    data class Item(val text: String, val categoryIndex: Int)
}

object CategorizeBuilder : GameBuilder<CategorizeRound> {
    override val id = GameId.Categorize

    override fun isAvailable(pack: StudyPack) = pack.categories.size >= 2

    override fun build(pack: StudyPack, settings: BuildSettings): CategorizeRound {
        val random = settings.random
        val groups = pack.categories.shuffled(random).take(settings.byDifficulty(2, 3, 4)).sortedBy(pack.categories::indexOf)
        val perGroup = settings.byDifficulty(3, 4, 5)
        val items = groups.flatMapIndexed { index, group ->
            group.members.shuffled(random).take(perGroup).map { CategorizeRound.Item(it, index) }
        }
        return CategorizeRound(groups.map { it.name }, items.shuffled(random))
    }
}

/** Spot the item that doesn't belong. */
data class OddOneOutRound(val sets: List<Set>) : GameRound {
    override val gameId = GameId.OddOneOut

    data class Set(val items: List<String>, val oddIndex: Int, val reason: String)
}

object OddOneOutBuilder : GameBuilder<OddOneOutRound> {
    override val id = GameId.OddOneOut

    override fun isAvailable(pack: StudyPack) = pack.oddSets.size >= 2

    override fun build(pack: StudyPack, settings: BuildSettings): OddOneOutRound {
        val random = settings.random
        val sets = pack.oddSets.shuffled(random).take(settings.byDifficulty(4, 6, 8)).map { set ->
            val items = set.items.shuffled(random)
            OddOneOutRound.Set(items, items.indexOf(set.odd), set.reason)
        }
        return OddOneOutRound(sets)
    }
}
