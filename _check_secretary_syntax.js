const fs = require("fs");
const html = fs.readFileSync("secretary.html", "utf8");
const regex = /<script\b([^>]*)>([\s\S]*?)<\/script>/gi;
let match;
const scripts = [];
while ((match = regex.exec(html))) {
  const attrs = match[1] || "";
  if (/\bsrc\s*=|application\/ld\+json/i.test(attrs)) continue;
  scripts.push(match[2]);
}
if (!scripts.length) throw new Error("No inline scripts found");
fs.writeFileSync("_secretary_pdf_syntax_check.js", scripts.join("\n;\n"));
console.log("Inline scripts extracted:", scripts.length);
