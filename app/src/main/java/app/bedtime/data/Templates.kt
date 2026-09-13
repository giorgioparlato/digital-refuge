package app.bedtime.data

enum class TemplateIcon { MOON, TARGET, SUN, LEAF, HEADPHONES, HOURGLASS, BOOK }

/** A ready-made starting point; [build] pre-selects the common distracting apps that are installed. */
class Template(
    val key: String,
    val kind: ScheduleKind,
    val name: String,
    val blurb: String,
    val icon: TemplateIcon,
    private val make: (distractions: Set<String>) -> Schedule,
) {
    fun build(isInstalled: (String) -> Boolean): Schedule = make(Templates.DISTRACTIONS.filter(isInstalled).toSet())
}

object Templates {
    const val SCRATCH_SCHEDULE = "scratch-schedule"
    const val SCRATCH_BLOCK = "scratch-block"

    /** Popular feeds and social apps, pre-selected as "blocked" when installed. */
    val DISTRACTIONS = listOf(
        "com.instagram.android",
        "com.instagram.barcelona", // Threads
        "com.zhiliaoapp.musically", // TikTok
        "com.ss.android.ugc.trill", // TikTok (some regions)
        "com.google.android.youtube",
        "com.facebook.katana",
        "com.twitter.android",
        "com.reddit.frontpage",
        "com.snapchat.android",
    )

    val all: List<Template> = listOf(
        Template("bedtime", ScheduleKind.RECURRING, "Bedtime", "greyscale · calm home screen · quiet phone", TemplateIcon.MOON) { apps ->
            Schedule(
                name = "Bedtime",
                startMinute = 22 * 60 + 30,
                endMinute = 7 * 60,
                blockedApps = apps,
                greyscale = true,
                minimalMode = true,
                dnd = DndMode.PRIORITY,
                hideNotifications = true,
                unlock = UnlockConfig(waitEnabled = true, waitMinutes = 10, textEnabled = true, textLength = 150),
            )
        },
        Template("work", ScheduleKind.RECURRING, "Work focus", "no social apps · notifications held back", TemplateIcon.TARGET) { apps ->
            Schedule(
                name = "Work focus",
                days = (1..5).toSet(),
                startMinute = 9 * 60,
                endMinute = 12 * 60,
                blockedApps = apps,
                greyscale = false,
                hideNotifications = true,
                unlock = UnlockConfig(waitEnabled = true, waitMinutes = 5, textEnabled = false),
                unlockAction = UnlockAction(UnlockMode.PAUSE, pauseMinutes = 10),
            )
        },
        Template("morning", ScheduleKind.RECURRING, "Slow morning", "greyscale · no feeds before breakfast", TemplateIcon.SUN) { apps ->
            Schedule(
                name = "Slow morning",
                startMinute = 7 * 60,
                endMinute = 9 * 60,
                blockedApps = apps,
                greyscale = true,
                unlock = UnlockConfig(textEnabled = true, textLength = 100),
            )
        },
        Template("weekend", ScheduleKind.RECURRING, "Weekend detox", "minimal home screen · greyscale", TemplateIcon.LEAF) { apps ->
            Schedule(
                name = "Weekend detox",
                days = setOf(6, 7),
                startMinute = 10 * 60,
                endMinute = 18 * 60,
                blockedApps = apps,
                greyscale = true,
                minimalMode = true,
                unlock = UnlockConfig(waitEnabled = true, waitMinutes = 15, textEnabled = true, textLength = 200),
            )
        },
        Template("focus", ScheduleKind.BLOCK, "Focus", "social apps blocked · quiet phone", TemplateIcon.TARGET) { apps ->
            Schedule(
                name = "Focus",
                kind = ScheduleKind.BLOCK,
                durationMinutes = 60,
                blockedApps = apps,
                greyscale = false,
                dnd = DndMode.PRIORITY,
                hideNotifications = true,
                unlock = UnlockConfig(waitEnabled = true, waitMinutes = 5, textEnabled = true, textLength = 100),
            )
        },
        Template("deepwork", ScheduleKind.BLOCK, "Deep work", "minimal home screen · silent phone, music still plays", TemplateIcon.HEADPHONES) { _ ->
            Schedule(
                name = "Deep work",
                kind = ScheduleKind.BLOCK,
                durationMinutes = 90,
                greyscale = true,
                minimalMode = true,
                dnd = DndMode.SILENCE,
                hideNotifications = true,
                unlock = UnlockConfig(waitEnabled = true, waitMinutes = 10, textEnabled = true, textLength = 200),
            )
        },
        Template("pomodoro", ScheduleKind.BLOCK, "Pomodoro", "one task, then a break", TemplateIcon.HOURGLASS) { apps ->
            Schedule(
                name = "Pomodoro",
                kind = ScheduleKind.BLOCK,
                durationMinutes = 25,
                blockedApps = apps,
                greyscale = false,
                hideNotifications = true,
                unlock = UnlockConfig(textEnabled = true, textLength = 75),
            )
        },
        Template("study", ScheduleKind.BLOCK, "Study session", "greyscale · notifications held back", TemplateIcon.BOOK) { apps ->
            Schedule(
                name = "Study session",
                kind = ScheduleKind.BLOCK,
                durationMinutes = 45,
                blockedApps = apps,
                greyscale = true,
                dnd = DndMode.PRIORITY,
                hideNotifications = true,
                unlock = UnlockConfig(waitEnabled = true, waitMinutes = 5, textEnabled = true, textLength = 100),
            )
        },
    )

    fun byKey(key: String): Template? = all.firstOrNull { it.key == key }

    /**
     * A new, unsaved schedule for a template key or one of the "from scratch" keys. [essentials]
     * (Settings, Phone, Messages) start out allowed, so minimal mode always leaves a way out.
     */
    fun instantiate(key: String, isInstalled: (String) -> Boolean, essentials: Set<String> = emptySet()): Schedule? {
        val schedule = when (key) {
            SCRATCH_SCHEDULE -> Schedule(name = "")
            SCRATCH_BLOCK -> Schedule(name = "", kind = ScheduleKind.BLOCK, greyscale = false)
            else -> byKey(key)?.build(isInstalled)
        }
        return schedule?.copy(allowedApps = schedule.allowedApps + essentials)
    }
}
