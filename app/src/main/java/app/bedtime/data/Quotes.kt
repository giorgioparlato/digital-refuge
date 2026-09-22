package app.bedtime.data

import java.time.LocalDate
import java.time.LocalDateTime

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
            "In some Native languages the term for plants translates to 'those who take care of us.'",
            "Robin Wall Kimmerer", "Braiding Sweetgrass",
        ),
        Quote(
            "In the presence of nature, a wild delight runs through the man, in spite of real sorrows.",
            "Ralph Waldo Emerson", "Nature",
        ),
        Quote(
            "Knowing that you love the earth changes you, activates you to defend and protect and celebrate. But when you " +
            "feel that the earth loves you in return, that feeling transforms the relationship from a one-way street into " +
            "a sacred bond.",
            "Robin Wall Kimmerer", "Braiding Sweetgrass",
        ),
        Quote("Know that life can only be found in the present moment.", "Thich Nhat Hanh", "Peace Is Every Step"),
        Quote(
            "He cannot be happy and strong until he too lives with nature in the present, above time.",
            "Ralph Waldo Emerson", "Self-Reliance",
        ),
        Quote("We can never have enough of Nature.", "Henry David Thoreau", "Walden"),
        Quote(
            "Recognizing 'enoughness' is a radical act in an economy that is always urging us to consume more.",
            "Robin Wall Kimmerer", "The Serviceberry",
        ),
        Quote(
            "When you begin to see that your enemy is suffering, that is the beginning of insight.",
            "Thich Nhat Hanh", "Peace Is Every Step",
        ),
        Quote(
            "One of the illusions is that the present hour is not the critical, decisive hour. Write it on your heart " +
            "that every day is the best day in the year.",
            "Ralph Waldo Emerson", "Works and Days",
        ),
        Quote("The question is not what you look at, but what you see.", "Henry David Thoreau", "Journal"),
        Quote(
            "Our indigenous herbalists say to pay attention when plants come to you; they're bringing you something you " +
            "need to learn.",
            "Robin Wall Kimmerer", "Braiding Sweetgrass",
        ),
        Quote(
            "If you only read the books that everyone else is reading, you can only think what everyone else is thinking.",
            "Haruki Murakami", "Norwegian Wood",
        ),
        Quote(
            "Breathing in, I calm body and mind. Breathing out, I smile. Dwelling in the present moment, I know this is " +
            "the only moment.",
            "Thich Nhat Hanh",
        ),
        Quote("He only is rich who owns the day.", "Ralph Waldo Emerson", "Works and Days"),
        Quote("In Wildness is the preservation of the World.", "Henry David Thoreau", "Walking"),
        Quote(
            "The land is the real teacher. All we need as students is mindfulness.",
            "Robin Wall Kimmerer", "Braiding Sweetgrass",
        ),
        Quote(
            "The most important thing we learn at school is the fact that the most important things can't be learned at " +
            "school.",
            "Haruki Murakami", "What I Talk About When I Talk About Running",
        ),
        Quote(
            "When you love someone, the best thing you can offer is your presence. How can you love if you are not there?",
            "Thich Nhat Hanh", "No Death, No Fear",
        ),
        Quote("Nature always wears the colors of the spirit.", "Ralph Waldo Emerson", "Nature"),
        Quote(
            "In my walks I would fain return to my senses. What business have I in the woods, if I am thinking of " +
            "something out of the woods?",
            "Henry David Thoreau", "Walking",
        ),
        Quote(
            "Those who are unhappy have no need for anything in this world but people capable of giving them their " +
            "attention.",
            "Simone Weil", "Waiting for God",
        ),
        Quote(
            "Ceremony focuses attention so that attention becomes intention.",
            "Robin Wall Kimmerer", "Braiding Sweetgrass",
        ),
        Quote("Thought creates the world and then says, 'I didn't do it.'", "David Bohm"),
        Quote(
            "We're both looking at the same moon, in the same world. We're connected to reality by the same line.",
            "Haruki Murakami", "Sputnik Sweetheart",
        ),
        Quote(
            "The real miracle is not to walk either on water or in thin air, but to walk on earth.",
            "Thich Nhat Hanh", "The Miracle of Mindfulness",
        ),
        Quote("In the woods, we return to reason and faith.", "Ralph Waldo Emerson", "Nature"),
        Quote(
            "The cost of a thing is the amount of what I will call life which is required to be exchanged for it, " +
            "immediately or in the long run.",
            "Henry David Thoreau", "Walden",
        ),
        Quote("Absolutely unmixed attention is prayer.", "Simone Weil", "Gravity and Grace"),
        Quote("In a consumer society, contentment is a radical proposition.", "Robin Wall Kimmerer", "Braiding Sweetgrass"),
        Quote(
            "Even as a mother protects with her life her child, her only child, so with a boundless heart should one " +
            "cherish all living beings.",
            "the Buddha", "Karaṇīya Mettā Sutta",
        ),
        Quote("How we spend our days is, of course, how we spend our lives.", "Annie Dillard", "The Writing Life"),
        Quote(
            "Attention is a moral act: it creates, brings aspects of things into being, but in doing so makes others " +
            "recede.",
            "Iain McGilchrist", "The Master and His Emissary",
        ),
        Quote("To pay attention, this is our endless and proper work.", "Mary Oliver"),
        Quote(
            "The love that consists in this: that two solitudes protect and border and greet each other.",
            "Rainer Maria Rilke", "Letters to a Young Poet",
        ),
        Quote(
            "The more clearly we can focus our attention on the wonders and realities of the universe about us, the less " +
            "taste we shall have for destruction.",
            "Rachel Carson",
        ),
        Quote(
            "Climb the mountains and get their good tidings. Nature's peace will flow into you as sunshine flows into " +
            "trees.",
            "John Muir", "Our National Parks",
        ),
        Quote(
            "You give but little when you give of your possessions. It is when you give of yourself that you truly give.",
            "Kahlil Gibran", "The Prophet",
        ),
        Quote(
            "Out beyond ideas of wrongdoing and rightdoing, there is a field. I'll meet you there.",
            "Rumi", "version by Coleman Barks",
        ),
        Quote(
            "Awareness, awareness, awareness! In awareness is healing; in awareness is truth; in awareness is salvation; " +
            "in awareness is love; in awareness is awakening.",
            "Anthony de Mello", "Awareness",
        ),
        Quote(
            "Communication can lead to the creation of something new only if people are able to freely listen to each " +
            "other, without prejudice.",
            "David Bohm", "On Dialogue",
        ),
        Quote(
            "It's all a question of imagination. Our responsibility begins with the power to imagine.",
            "Haruki Murakami", "Kafka on the Shore",
        ),
        Quote(
            "Beauty is the harvest of presence, the evanescent moment of seeing or hearing on the outside what already " +
            "lives far inside us.",
            "David Whyte", "Consolations",
        ),
        Quote(
            "The most precious gift we can offer anyone is our attention. When mindfulness embraces those we love, they " +
            "will bloom like flowers.",
            "Thich Nhat Hanh",
        ),
        Quote("The invariable mark of wisdom is to see the miraculous in the common.", "Ralph Waldo Emerson", "Nature"),
        Quote(
            "I have a great deal of company in my house; especially in the morning, when nobody calls.",
            "Henry David Thoreau", "Walden",
        ),
        Quote("Attention is the rarest and purest form of generosity.", "Simone Weil", "letter to Joë Bousquet"),
        Quote(
            "Paying attention is a form of reciprocity with the living world, receiving the gifts with open eyes and open " +
            "heart.",
            "Robin Wall Kimmerer", "Braiding Sweetgrass",
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
        Quote(
            "Silent friend of many distances, feel how your breath enlarges all of space.",
            "Rainer Maria Rilke", "Sonnets to Orpheus",
        ),
        Quote(
            "Those who contemplate the beauty of the earth find reserves of strength that will endure as long as life " +
            "lasts.",
            "Rachel Carson", "The Sense of Wonder",
        ),
        Quote("In every walk with Nature one receives far more than he seeks.", "John Muir", "Steep Trails"),
        Quote(
            "And forget not that the earth delights to feel your bare feet and the winds long to play with your hair.",
            "Kahlil Gibran", "The Prophet",
        ),
        Quote("The breeze at dawn has secrets to tell you. Don't go back to sleep.", "Rumi", "The Essential Rumi"),
        Quote(
            "There are no unsacred places; there are only sacred places and desecrated places.",
            "Wendell Berry", "How to Be a Poet",
        ),
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
            "We abuse land because we regard it as a commodity belonging to us. When we see land as a community to which " +
            "we belong, we may begin to use it with love and respect.",
            "Aldo Leopold", "A Sand County Almanac",
        ),
        Quote("Just to be is a blessing. Just to live is holy.", "Abraham Joshua Heschel"),
        Quote(
            "All that you touch you change. All that you change changes you.",
            "Octavia E. Butler", "Parable of the Sower",
        ),
        Quote("Where there is love there are no demands, no expectations, no dependency.", "Anthony de Mello", "Awareness"),
        Quote(
            "If he thinks of the totality as constituted of independent fragments, then that is how his mind will tend to " +
            "operate.",
            "David Bohm", "Wholeness and the Implicate Order",
        ),
        Quote(
            "To be a participant in a complex system is to desire to be both lost and found in the interrelationships " +
            "between people, nature and ideas.",
            "Nora Bateson", "Small Arcs of Larger Circles",
        ),
        Quote(
            "Spend your money on the things money can buy. Spend your time on the things money can't buy.",
            "Haruki Murakami", "The Wind-Up Bird Chronicle",
        ),
        Quote(
            "Gratitude is not a passive response to something we have been given; gratitude arises from paying attention.",
            "David Whyte", "Consolations",
        ),
        Quote(
            "Open my grave when I am dead, and thou shalt see a cloud of smoke rising out from it; then shalt thou know " +
            "that the fire still burns in my dead heart.",
            "Hafiz", "tr. Gertrude Bell",
        ),
        Quote("Between stimulus and response there is a space.", "Viktor Frankl", "attributed"),
        Quote("What we plant in the soil of contemplation we shall reap in the harvest of action.", "Meister Eckhart"),
    )

    /** Today's quote; [offset] steps to the following ones ("tap for another"). */
    fun forDay(date: LocalDate, offset: Int = 0): Quote =
        all[Math.floorMod(date.toEpochDay() + offset, all.size.toLong()).toInt()]

    /**
     * The quote for the stretch of time [now] falls in, so it changes as often as [refresh] says.
     * At [QuoteRefresh.DAILY] this matches [forDay]: a new one at local midnight.
     */
    fun forPeriod(now: LocalDateTime, refresh: QuoteRefresh, offset: Int = 0): Quote {
        val hours = now.toLocalDate().toEpochDay() * 24 + now.hour
        val period = Math.floorDiv(hours, refresh.hours.toLong())
        return all[Math.floorMod(period + offset, all.size.toLong()).toInt()]
    }
}
