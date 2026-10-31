"""Copies the microjournal's suggested sections from docs/text/journal.md into data/Journal.kt, and the
"ask why" questions (after unlocking, before a limit's apps) into data/Why.kt, and the cover messages
into data/CoverMessages.kt.

Run from the project folder: python3 -I docs/text/journal_to_code.py .
Each "### Name" under "## Microjournal sections" is a section: its "**Guide:**" line, then each
"- question" with its example as an indented "- *example*" line under it.
"""
import re
import sys

root = sys.argv[1] if len(sys.argv) > 1 else "."
md = open(f"{root}/docs/text/journal.md", encoding="utf-8").read()
part = md.split("## Microjournal sections", 1)[1].split("\n---", 1)[0]

# Sections keep the ids they've had, so unlock steps that name them still find them.
IDS = {"clarity": "clarity", "pattern interrupt": "fixation", "daily reflections": "reflection", "when bad things happen": "bad",
       "anxieties": "anxiety", "to-do list and direction": "todo", "making a decision": "decision", "direction in life": "direction",
       "self compassion": "compassion"}


def kt(text):
    # Everything in refuge is lowercase, wherever it's shown or written: whatever the doc's capitals.
    text = text.replace("*", "").strip().lower()
    return '"' + text.replace("\\", "\\\\").replace('"', '\\"') + '"'


sections = []
for chunk in part.split("\n### ")[1:]:
    lines = chunk.split("\n")
    name = lines[0].strip()
    guide = ""
    prompts = []
    for line in lines[1:]:
        if line.startswith("**Guide:**"):
            guide = line[len("**Guide:**"):].strip()
        elif line.startswith("- "):
            prompts.append([line[2:].strip(), None])
        elif line.startswith("  - ") and prompts:
            prompts[-1][1] = line[4:].strip()
    sid = IDS.get(name.lower(), re.sub(r"[^a-z0-9]+", "-", name.lower()).strip("-"))
    sections.append((sid, name, guide, prompts))

out = ["    val SUGGESTED: List<JournalSection> = listOf("]
for sid, name, guide, prompts in sections:
    out.append("        section(")
    out.append(f'            "{sid}", {kt(name)},')
    out.append(f"            {kt(guide)},")
    for q, h in prompts:
        out.append(f"            {kt(q)} to {kt(h) if h else 'null'},")
    out.append("        ),")
out.append("    )")

path = f"{root}/app/src/main/java/app/bedtime/data/Journal.kt"
code = open(path, encoding="utf-8").read()
start = code.index("    val SUGGESTED: List<JournalSection> = listOf(")
end = code.index("\n    )\n", start) + len("\n    )")
open(path, "w", encoding="utf-8").write(code[:start] + "\n".join(out) + code[end:])
print(len(sections), "sections,", sum(len(p) for *_, p in sections), "questions")

# "Ask why": the two lists of questions, each a "- question" line under its own heading.
why_part = md.split('## "Ask why"', 1)[1]


def questions(heading):
    block = why_part.split(heading, 1)[1].split("\n### ", 1)[0]
    return [line[2:].strip() for line in block.split("\n") if line.startswith("- ")]


why_path = f"{root}/app/src/main/java/app/bedtime/data/Why.kt"
why = open(why_path, encoding="utf-8").read()
for name, heading in (("QUESTIONS", "### After unlocking"), ("APP_QUESTIONS", "### Before a limit's apps")):
    start = why.index(f"    val {name} = listOf(")
    end = why.index("\n    )\n", start) + len("\n    )")
    body = "\n".join(f"        {kt(q)}," for q in questions(heading))
    why = why[:start] + f"    val {name} = listOf(\n{body}\n    )" + why[end:]
open(why_path, "w", encoding="utf-8").write(why)
print(len(questions("### After unlocking")), "questions after unlocking,", len(questions("### Before a limit's apps")), "before apps")

# Cover messages: the "- message" lines under their heading.
cover_block = md.split("## Cover messages", 1)[1].split("\n---", 1)[0]
covers = [line[2:].strip() for line in cover_block.split("\n") if line.startswith("- ")]
cover_path = f"{root}/app/src/main/java/app/bedtime/data/CoverMessages.kt"
code = open(cover_path, encoding="utf-8").read()
start = code.index("    val presets: List<String> = listOf(")
end = code.index("\n    )\n", start) + len("\n    )")
body = "\n".join(f"        {kt(m)}," for m in covers)
code = code[:start] + f"    val presets: List<String> = listOf(\n{body}\n    )" + code[end:]
open(cover_path, "w", encoding="utf-8").write(code)
print(len(covers), "cover messages")
