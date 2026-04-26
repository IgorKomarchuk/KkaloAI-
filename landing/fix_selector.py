import os, re

pages = [
    'index.html','instant-snap.html','3d-scanner.html','ai-coaching.html',
    'glp1-companion.html','health-connect.html','hidden-fats.html',
    'streaks.html','viral-share.html','privacy.html','contact.html'
]

new_selector = (
    '<select onchange="window.setKkaloLanguage(this.value);" '
    'style="background: rgba(17, 24, 39, 0.9); color: var(--text-main); '
    "border: 1px solid var(--card-border); padding: 8px 12px; border-radius: 8px; "
    "font-family: 'Outfit'; font-weight: 600; cursor: pointer; outline: none;\">\n"
    "                <option value=\"en\" selected>\U0001f1ec\U0001f1e7 EN</option>\n"
    "                <option value=\"uk\">\U0001f1fa\U0001f1e6 UK</option>\n"
    "            </select>"
)

for fname in pages:
    if not os.path.exists(fname):
        continue
    c = open(fname, encoding='utf-8').read()
    new_c = re.sub(
        r'<select onchange="window\.setKkaloLanguage[^>]*>.*?</select>',
        new_selector, c, flags=re.DOTALL
    )
    if new_c != c:
        open(fname, 'w', encoding='utf-8').write(new_c)
        print(f'Updated: {fname}')
    else:
        print(f'No selector found: {fname}')

print('Done')
