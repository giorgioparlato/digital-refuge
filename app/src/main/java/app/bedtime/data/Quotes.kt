package app.bedtime.data

import java.time.LocalDate

/** A short line for the minimal home and the lock screen. [work] is null when no book could be confirmed. */
data class Quote(val text: String, val author: String, val work: String? = null)

/**
 * The quotes chosen in docs/quotes-review.md and docs/quotes-review-2.md, each checked against its
 * source there. Only add quotes that have been verified the same way.
 */
object Quotes {
    /** Ordered so neighbouring days never share an author. */
    val all: List<Quote> = listOf(
        Quote(
            "The most precious gift we can offer anyone is our attention. When mindfulness embraces those we love, they will bloom " +
                "like flowers.",
            "Thich Nhat Hanh",
        ),
        Quote("The invariable mark of wisdom is to see the miraculous in the common.", "Ralph Waldo Emerson", "Nature"),
        Quote("I have a great deal of company in my house; especially in the morning, when nobody calls.", "Henry David Thoreau", "Walden"),
        Quote("The land knows you, even when you are lost.", "Robin Wall Kimmerer", "Braiding Sweetgrass"),
        Quote("Attention is the rarest and purest form of generosity.", "Simone Weil", "letter to Joë Bousquet"),
        Quote(
            "Paying attention is a form of reciprocity with the living world, receiving the gifts with open eyes and open heart.",
            "Robin Wall Kimmerer", "Braiding Sweetgrass",
        ),
        Quote("In the woods, we return to reason and faith.", "Ralph Waldo Emerson", "Nature"),
        Quote(
            "The cost of a thing is the amount of what I will call life which is required to be exchanged for it, immediately or in " +
                "the long run.",
            "Henry David Thoreau", "Walden",
        ),
        Quote(
            "Whatever a monk keeps pursuing with his thinking & pondering, that becomes the inclination of his awareness.",
            "the Buddha", "Dvedhāvitakka Sutta",
        ),
        Quote(
            "Beauty and grace are performed whether or not we will or sense them. The least we can do is try to be there.",
            "Annie Dillard", "Pilgrim at Tinker Creek",
        ),
        Quote(
            "The kind of attention we pay actually alters the world: we are, literally, partners in creation.",
            "Iain McGilchrist", "The Master and His Emissary",
        ),
        Quote("Attention is the beginning of devotion.", "Mary Oliver", "Upstream"),
        Quote("Silent friend of many distances, feel how your breath enlarges all of space.", "Rainer Maria Rilke", "Sonnets to Orpheus"),
        Quote(
            "Those who contemplate the beauty of the earth find reserves of strength that will endure as long as life lasts.",
            "Rachel Carson", "The Sense of Wonder",
        ),
        Quote("In every walk with Nature one receives far more than he seeks.", "John Muir", "Steep Trails"),
        Quote(
            "And forget not that the earth delights to feel your bare feet and the winds long to play with your hair.",
            "Kahlil Gibran", "The Prophet",
        ),
        Quote("In a consumer society, contentment is a radical proposition.", "Robin Wall Kimmerer", "Braiding Sweetgrass"),
        Quote("Nature always wears the colors of the spirit.", "Ralph Waldo Emerson", "Nature"),
        Quote("Ceremony focuses attention so that attention becomes intention.", "Robin Wall Kimmerer", "Braiding Sweetgrass"),
        Quote(
            "In my walks I would fain return to my senses. What business have I in the woods, if I am thinking of something out of " +
                "the woods?",
            "Henry David Thoreau", "Walking",
        ),
        Quote("The land is the real teacher. All we need as students is mindfulness.", "Robin Wall Kimmerer", "Braiding Sweetgrass"),
        Quote("He only is rich who owns the day.", "Ralph Waldo Emerson", "Works and Days"),
        Quote("Absolutely unmixed attention is prayer.", "Simone Weil", "Gravity and Grace"),
        Quote(
            "The real miracle is not to walk either on water or in thin air, but to walk on earth.",
            "Thich Nhat Hanh", "The Miracle of Mindfulness",
        ),
        Quote("The breeze at dawn has secrets to tell you. Don't go back to sleep.", "Rumi", "The Essential Rumi"),
        Quote("There are no unsacred places; there are only sacred places and desecrated places.", "Wendell Berry", "How to Be a Poet"),
        Quote(
            "My experience is what I agree to attend to. Only those items which I notice shape my mind.",
            "William James", "The Principles of Psychology",
        ),
        Quote("how things appear always depends on how I look.", "Rob Burbea", "Seeing That Frees"),
        Quote(
            "Life is nothing but a dream, and if we are artists, then we can create our life with Love.",
            "don Miguel Ruiz", "The Mastery of Love",
        ),
        Quote(
            "It is with acts of attention that we decide who to hear, who to see, and who in our world has agency.",
            "Jenny Odell", "How to Do Nothing",
        ),
        Quote(
            "We abuse land because we regard it as a commodity belonging to us. When we see land as a community to which we belong, " +
                "we may begin to use it with love and respect.",
            "Aldo Leopold", "A Sand County Almanac",
        ),
        Quote("Just to be is a blessing. Just to live is holy.", "Abraham Joshua Heschel"),
        Quote("In Wildness is the preservation of the World.", "Henry David Thoreau", "Walking"),
        Quote(
            "Our indigenous herbalists say to pay attention when plants come to you; they're bringing you something you need to learn.",
            "Robin Wall Kimmerer", "Braiding Sweetgrass",
        ),
        Quote(
            "One of the illusions is that the present hour is not the critical, decisive hour. Write it on your heart that every day " +
                "is the best day in the year.",
            "Ralph Waldo Emerson", "Works and Days",
        ),
        Quote(
            "Recognizing 'enoughness' is a radical act in an economy that is always urging us to consume more.",
            "Robin Wall Kimmerer", "The Serviceberry",
        ),
        Quote("The question is not what you look at, but what you see.", "Henry David Thoreau", "Journal"),
        Quote(
            "Even as a mother protects with her life her child, her only child, so with a boundless heart should one cherish all " +
                "living beings.",
            "the Buddha", "Karaṇīya Mettā Sutta",
        ),
        Quote("How we spend our days is, of course, how we spend our lives.", "Annie Dillard", "The Writing Life"),
        Quote(
            "Attention is a moral act: it creates, brings aspects of things into being, but in doing so makes others recede.",
            "Iain McGilchrist", "The Master and His Emissary",
        ),
        Quote("To pay attention, this is our endless and proper work.", "Mary Oliver"),
        Quote(
            "The love that consists in this: that two solitudes protect and border and greet each other.",
            "Rainer Maria Rilke", "Letters to a Young Poet",
        ),
        Quote(
            "The more clearly we can focus our attention on the wonders and realities of the universe about us, the less taste we " +
                "shall have for destruction.",
            "Rachel Carson",
        ),
        Quote(
            "Climb the mountains and get their good tidings. Nature's peace will flow into you as sunshine flows into trees.",
            "John Muir", "Our National Parks",
        ),
        Quote(
            "You give but little when you give of your possessions. It is when you give of yourself that you truly give.",
            "Kahlil Gibran", "The Prophet",
        ),
        Quote(
            "He cannot be happy and strong until he too lives with nature in the present, above time.",
            "Ralph Waldo Emerson", "Self-Reliance",
        ),
        Quote(
            "Those who are unhappy have no need for anything in this world but people capable of giving them their attention.",
            "Simone Weil", "Waiting for God",
        ),
        Quote(
            "When you love someone, the best thing you can offer is your presence. How can you love if you are not there?",
            "Thich Nhat Hanh", "No Death, No Fear",
        ),
        Quote(
            "Knowing that you love the earth changes you, activates you to defend and protect and celebrate. But when you feel that " +
                "the earth loves you in return, that feeling transforms the relationship from a one-way street into a sacred bond.",
            "Robin Wall Kimmerer", "Braiding Sweetgrass",
        ),
        Quote("We can never have enough of Nature.", "Henry David Thoreau", "Walden"),
        Quote("In the presence of nature, a wild delight runs through the man, in spite of real sorrows.", "Ralph Waldo Emerson", "Nature"),
        Quote(
            "In some Native languages the term for plants translates to 'those who take care of us.'",
            "Robin Wall Kimmerer", "Braiding Sweetgrass",
        ),
    )

    /** Today's quote; [offset] steps to the following ones ("tap for another"). */
    fun forDay(date: LocalDate, offset: Int = 0): Quote =
        all[Math.floorMod(date.toEpochDay() + offset, all.size.toLong()).toInt()]
}
