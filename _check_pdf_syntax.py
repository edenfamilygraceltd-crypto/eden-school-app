from pathlib import Path
import re
s = Path('comptable.html').read_text(encoding='utf-8')
blocks = []
for m in re.finditer(r'<script([^>]*)>([\s\S]*?)</script>', s, re.I):
    if 'application/ld+json' not in m.group(1).lower():
        blocks.append(m.group(2))
Path('_check_comptable_pdf.js').write_text('\n'.join(blocks), encoding='utf-8')
print('extracted', len(blocks), 'script blocks')
