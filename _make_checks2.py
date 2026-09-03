from pathlib import Path
import re
for src,out in [('comptable.html','_check_comptable.js'),('secretary.html','_check_secretary.js')]:
    t=Path(src).read_text(encoding='utf-8')
    chunks=[]
    for m in re.finditer(r'<script([^>]*)>(.*?)</script>',t,re.S|re.I):
        attrs,body=m.group(1),m.group(2)
        if 'application/ld+json' not in attrs.lower(): chunks.append(body)
    Path(out).write_text('\n'.join(chunks),encoding='utf-8')
print('checks created')