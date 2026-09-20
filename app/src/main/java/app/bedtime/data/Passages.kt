package app.bedtime.data

/**
 * Longer texts to copy out during a typing challenge, and the word list behind the "words" option.
 *
 * The passages are the Tao Te Ching selections from docs/quotes-review-2.md (Stephen Mitchell's
 * version, 1988), checked line by line against the source there. They're too long to sit on the
 * minimal home screen as quotes, but typing one out is exactly the sort of pause this app is for.
 */
object Passages {
    /** Tao Te Ching, tr. Stephen Mitchell. Chapter numbers kept for anyone checking them. */
    val TAO: List<String> = listOf(
        // 12
        "Colors blind the eye. Sounds deafen the ear. Flavors numb the taste. Thoughts weaken the mind. " +
            "Desires wither the heart. The Master observes the world but trusts his inner vision. He allows things " +
            "to come and go. His heart is open as the sky.",
        // 10
        "Can you coax your mind from its wandering and keep to the original oneness? Can you let your body become " +
            "supple as a newborn child's? Can you cleanse your inner vision until you see nothing but the light? " +
            "Can you love people and lead them without imposing your will? Can you deal with the most vital matters " +
            "by letting events take their course? Can you step back from your own mind and thus understand all things?",
        // 16
        "Empty your mind of all thoughts. Let your heart be at peace. Watch the turmoil of beings, but contemplate " +
            "their return. Each separate being in the universe returns to the common source. Returning to the source " +
            "is serenity.",
        // 15
        "Do you have the patience to wait till your mud settles and the water is clear? Can you remain unmoving till " +
            "the right action arises by itself? The Master doesn't seek fulfillment. Not seeking, not expecting, she " +
            "is present, and can welcome all things.",
        // 56
        "Those who know don't talk. Those who talk don't know. Close your mouth, block off your senses, blunt your " +
            "sharpness, untie your knots, soften your glare, settle your dust. This is the primal identity.",
        // 47
        "Without opening your door, you can open your heart to the world. Without looking out your window, you can " +
            "see the essence of the Tao. The more you know, the less you understand. The Master arrives without " +
            "leaving, sees the light without looking, achieves without doing a thing.",
        // 48
        "In pursuit of knowledge, every day something is added. In the practice of the Tao, every day something is " +
            "dropped. Less and less do you need to force things, until finally you arrive at non-action. When nothing " +
            "is done, nothing is left undone.",
        // 8
        "In dwelling, live close to the ground. In thinking, keep to the simple. In conflict, be fair and generous. " +
            "In governing, don't try to control. In work, do what you enjoy. In family life, be completely present. " +
            "When you are content to be simply yourself and don't compare or compete, everybody will respect you.",
        // 67
        "I have just three things to teach: simplicity, patience, compassion. These three are your greatest " +
            "treasures. Simple in actions and in thoughts, you return to the source of being. Patient with both " +
            "friends and enemies, you accord with the way things are. Compassionate toward yourself, you reconcile " +
            "all beings in the world.",
        // 49
        "The Master has no mind of her own. She works with the mind of the people. She is good to people who are " +
            "good. She is also good to people who aren't good. This is true goodness.",
        // 81
        "True words aren't eloquent; eloquent words aren't true. Wise men don't need to prove their point; men who " +
            "need to prove their point aren't wise. The Master has no possessions. The more he does for others, the " +
            "happier he is. The more he gives to others, the wealthier he is.",
        // 29
        "Do you want to improve the world? I don't think it can be done. The world is sacred. It can't be improved. " +
            "If you tamper with it, you'll ruin it. If you treat it like an object, you'll lose it.",
        // 23
        "Express yourself completely, then keep quiet. Be like the forces of nature: when it blows, there is only " +
            "wind; when it rains, there is only rain; when the clouds pass, the sun shines through.",
        // 9
        "Fill your bowl to the brim and it will spill. Keep sharpening your knife and it will blunt. Chase after " +
            "money and security and your heart will never unclench. Care about people's approval and you will be " +
            "their prisoner. Do your work, then step back. The only path to serenity.",
        // 44
        "If you look to others for fulfillment, you will never truly be fulfilled. If your happiness depends on " +
            "money, you will never be happy with yourself. Be content with what you have; rejoice in the way things " +
            "are. When you realize there is nothing lacking, the whole world belongs to you.",
        // 33
        "Knowing others is intelligence; knowing yourself is true wisdom. Mastering others is strength; mastering " +
            "yourself is true power. If you realize that you have enough, you are truly rich. If you stay in the " +
            "center and embrace death with your whole heart, you will endure forever.",
        // 2
        "Things arise and she lets them come; things disappear and she lets them go. She has but doesn't possess, " +
            "acts but doesn't expect. When her work is done, she forgets it. That is why it lasts forever.",
        // 22
        "If you want to become whole, let yourself be partial. If you want to become straight, let yourself be " +
            "crooked. If you want to become full, let yourself be empty. If you want to be reborn, let yourself die. " +
            "If you want to be given everything, give everything up.",
        // 11
        "We join spokes together in a wheel, but it is the center hole that makes the wagon move. We shape clay into " +
            "a pot, but it is the emptiness inside that holds whatever we want. We hammer wood for a house, but it " +
            "is the inner space that makes it livable. We work with being, but non-being is what we use.",
        // 76
        "Men are born soft and supple; dead, they are stiff and hard. Plants are born tender and pliant; dead, they " +
            "are brittle and dry. Thus whoever is stiff and inflexible is a disciple of death. Whoever is soft and " +
            "yielding is a disciple of life.",
        // 63
        "Act without doing; work without effort. Think of the small as large and the few as many. Confront the " +
            "difficult while it is still easy; accomplish the great task by a series of small acts.",
    )

    /** Everything a "passage" challenge can draw from: the app's quotes, plus the longer passages. */
    val ALL: List<String> get() = Quotes.all.map { it.text } + TAO

    /** Plain, unhurried words; nothing you'd need to squint at on a keyboard at midnight. */
    val WORDS: List<String> = (
        "able about above across after again against air all almost alone along already also always among " +
            "ancient answer any appear around ask attention autumn away back balance basket become been before " +
            "begin behind being believe below beside best better between beyond bird blue boat body both branch " +
            "bread breath bridge bright bring broad brother build burn call calm candle care carry catch center " +
            "certain change child choose circle city clean clear climb close cloud cold color come common " +
            "continue cool corner country course cover create cross crowd curious dark daughter dawn day deep " +
            "desert distance door draw dream drink drive drop dry early earth ease east easy edge either else " +
            "empty end enough enter equal evening ever every exact example eye face fall family far farm father " +
            "feel few field fill find fire first fish flower fold follow food foot forest forget form forward " +
            "found free fresh friend front full garden gather gentle gift give glass go gold good grass great " +
            "green ground grow guest half hand happen hard harvest head hear heart heavy help here high hill " +
            "hold hollow home honest hope horse hour house human hundred hunger idea important inside island " +
            "join journey keep kind kitchen know land language last late laugh lay lead leaf learn leave left " +
            "less letter level lie life light like line listen little live long look loose lose loud love low " +
            "main make many mark market matter may meadow mean measure meet memory middle might mile milk mind " +
            "minute moment money month moon more morning most mother mountain mouth move much music must name " +
            "narrow near neck need never new news next night north note nothing notice now number ocean offer " +
            "often old once only open order other out over own page pair paper part pass past path patient " +
            "pause peace people perhaps person picture piece place plain plan plant play please point poor " +
            "possible pour power practice prepare present press pretty promise proper pull pure push put quiet " +
            "quick rain raise reach read ready real reason receive record red remain remember rest return rich " +
            "ride right ring rise river road rock roll roof room root rope round run safe salt same sand save " +
            "say school sea season seat second see seed seem sell send sense serve set settle several shade " +
            "shape share sharp sheep shelter shine ship shoe shop short should shoulder show side sight sign " +
            "silence silver simple since sing single sister sit six size sky sleep slow small smell smile smoke " +
            "snow soft soil some son song soon sound source south space speak spend spring square stand star " +
            "start stay steady step still stone stop store storm story straight strange stream street strong " +
            "study such sudden summer sun suppose sure surface sweet swim table take talk tall taste teach tell " +
            "ten than thank that their them then there these thick thin thing think third this those though " +
            "thought thread three through throw thus time tiny today together tomorrow tone tonight too tool " +
            "touch toward town track trade train travel tree true trust try turn twelve twenty two under " +
            "understand until upon use usual valley value very village visit voice wait walk wall want warm " +
            "watch water wave way wear weather week weight well west wet what wheat wheel when where which " +
            "while white who whole why wide wild will wind window wing winter wise wish with within without " +
            "woman wonder wood word work world would write year yellow yes yesterday yet young"
        ).split(" ")
}
