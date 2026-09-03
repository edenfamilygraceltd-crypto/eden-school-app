from pathlib import Path
import re
for src,out in [('comptable.html','_check_comptable.js'),('secretary.html','_check_secretary.js')]:
    t=Path(src).read_text(encoding='utf-8')
    s='\n'.join(re.findall(r'<script(?:\s[^>]*)?>(.*?)</script>',t,re.S))
    Path(out).write_text(s,encoding='utf-8')
print('checks created')