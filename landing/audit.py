import sys, os
sys.stdout.reconfigure(encoding='utf-8')
langs = ['uk', 'ru', 'es', 'it', 'de', 'fr', 'pl', 'hi']
checks = [
    "The World's 1st", "Beyond simple photos", "The core of KkaloAI", "Next-Gen Features",
    "The foundation of precision", "Instant AI Snap", "3D Portion Measurement",
    "GLP-1 Companion", "AI Cooking Detection", "Weekly AI Coaching", "Daily Streaks",
    "Biofeedback Sync", "Social Viral Loops", "Why thousands switched",
    "The Premium Experience", "Start Free Trial", "Learn More"
]
for lang in langs:
    f = os.path.join(lang, 'index.html')
    if not os.path.exists(f):
        print(f"{lang}: FILE MISSING")
        continue
    content = open(f, encoding='utf-8').read()
    remaining = [c for c in checks if c in content]
    status = "CLEAN" if not remaining else str(remaining[:5])
    print(f"{lang}: {len(remaining)} untranslated -> {status}")
