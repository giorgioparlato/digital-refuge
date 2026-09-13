package app.bedtime.data

import java.time.LocalDate

/** A short line for the minimal home and the lock screen. [work] is null when no book could be confirmed. */
data class Quote(val text: String, val author: String, val work: String? = null)

/**
 * The quotes chosen in docs/quotes-review.md, each checked against its source there.
 * Only add quotes that have been verified the same way.
 */
object Quotes {
    /** Ordered so neighbouring days never share an author. */
    val all: List<Quote> = listOf(
        Quote(
            "Paying attention is a form of reciprocity with the living world, receiving the gifts with open eyes and open heart.",
            "Robin Wall Kimmerer", "Braiding Sweetgrass",
        ),
        Quote("I have a great deal of company in my house; especially in the morning, when nobody calls.", "Henry David Thoreau", "Walden"),
        Quote("Attention is the rarest and purest form of generosity.", "Simone Weil", "letter to Joë Bousquet"),
        Quote("The invariable mark of wisdom is to see the miraculous in the common.", "Ralph Waldo Emerson", "Nature"),
        Quote(
            "Whatever a monk keeps pursuing with his thinking & pondering, that becomes the inclination of his awareness.",
            "the Buddha", "Dvedhāvitakka Sutta",
        ),
        Quote("The breeze at dawn has secrets to tell you. Don't go back to sleep.", "Rumi", "The Essential Rumi"),
        Quote("The land knows you, even when you are lost.", "Robin Wall Kimmerer", "Braiding Sweetgrass"),
        Quote(
            "Beauty and grace are performed whether or not we will or sense them. The least we can do is try to be there.",
            "Annie Dillard", "Pilgrim at Tinker Creek",
        ),
        Quote(
            "The cost of a thing is the amount of what I will call life which is required to be exchanged for it, immediately or in the long run.",
            "Henry David Thoreau", "Walden",
        ),
        Quote(
            "The kind of attention we pay actually alters the world: we are, literally, partners in creation.",
            "Iain McGilchrist", "The Master and His Emissary",
        ),
        Quote("In the woods, we return to reason and faith.", "Ralph Waldo Emerson", "Nature"),
        Quote("Attention is the beginning of devotion.", "Mary Oliver", "Upstream"),
        Quote("In a consumer society, contentment is a radical proposition.", "Robin Wall Kimmerer", "Braiding Sweetgrass"),
        Quote(
            "There are no unsacred places; there are only sacred places and desecrated places.",
            "Wendell Berry", "How to Be a Poet",
        ),
        Quote(
            "In my walks I would fain return to my senses. What business have I in the woods, if I am thinking of something out of the woods?",
            "Henry David Thoreau", "Walking",
        ),
        Quote("Silent friend of many distances, feel how your breath enlarges all of space.", "Rainer Maria Rilke", "Sonnets to Orpheus"),
        Quote("Nature always wears the colors of the spirit.", "Ralph Waldo Emerson", "Nature"),
        Quote(
            "My experience is what I agree to attend to. Only those items which I notice shape my mind.",
            "William James", "The Principles of Psychology",
        ),
        Quote("Ceremony focuses attention so that attention becomes intention.", "Robin Wall Kimmerer", "Braiding Sweetgrass"),
        Quote("how things appear always depends on how I look.", "Rob Burbea", "Seeing That Frees"),
        Quote("He only is rich who owns the day.", "Ralph Waldo Emerson", "Works and Days"),
        Quote(
            "Those who contemplate the beauty of the earth find reserves of strength that will endure as long as life lasts.",
            "Rachel Carson", "The Sense of Wonder",
        ),
        Quote(
            "Even as a mother protects with her life her child, her only child, so with a boundless heart should one cherish all living beings.",
            "the Buddha", "Karaṇīya Mettā Sutta",
        ),
        Quote("The land is the real teacher. All we need as students is mindfulness.", "Robin Wall Kimmerer", "Braiding Sweetgrass"),
        Quote("In Wildness is the preservation of the World.", "Henry David Thoreau", "Walking"),
        Quote("Absolutely unmixed attention is prayer.", "Simone Weil", "Gravity and Grace"),
        Quote(
            "One of the illusions is that the present hour is not the critical, decisive hour. Write it on your heart that every day is the best day in the year.",
            "Ralph Waldo Emerson", "Works and Days",
        ),
        Quote(
            "Life is nothing but a dream, and if we are artists, then we can create our life with Love.",
            "don Miguel Ruiz", "The Mastery of Love",
        ),
        Quote(
            "Our indigenous herbalists say to pay attention when plants come to you; they're bringing you something you need to learn.",
            "Robin Wall Kimmerer", "Braiding Sweetgrass",
        ),
        Quote("How we spend our days is, of course, how we spend our lives.", "Annie Dillard", "The Writing Life"),
        Quote(
            "Recognizing 'enoughness' is a radical act in an economy that is always urging us to consume more.",
            "Robin Wall Kimmerer", "The Serviceberry",
        ),
        Quote(
            "He cannot be happy and strong until he too lives with nature in the present, above time.",
            "Ralph Waldo Emerson", "Self-Reliance",
        ),
        Quote(
            "Knowing that you love the earth changes you, activates you to defend and protect and celebrate. But when you feel that the earth " +
                "loves you in return, that feeling transforms the relationship from a one-way street into a sacred bond.",
            "Robin Wall Kimmerer", "Braiding Sweetgrass",
        ),
        Quote(
            "The most precious gift we can offer anyone is our attention. When mindfulness embraces those we love, they will bloom like flowers.",
            "Thich Nhat Hanh",
        ),
    )

    /** Today's quote; [offset] steps to the following ones ("tap for another"). */
    fun forDay(date: LocalDate, offset: Int = 0): Quote =
        all[Math.floorMod(date.toEpochDay() + offset, all.size.toLong()).toInt()]
}
