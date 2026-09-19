from pathlib import Path
import re
p=Path(r"C:\Users\graph\Documents\directeur portaille\secretary.html")
t=p.read_text(encoding="utf-8")
parts=[]
for m in re.finditer(r"<script[^>]*>(.*?)</script>", t, re.I|re.S):
    tag=m.group(0).split(">",1)[0]
    if 'application/ld+json' in tag.lower():
        continue
    parts.append(m.group(1))
Path(r"C:\Users\graph\Documents\directeur portaille\_secretary_inline_check.js").write_text("\n".join(parts), encoding="utf-8")
print("scripts:",len(parts))