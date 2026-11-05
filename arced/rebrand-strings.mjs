// Replaces the "Bitwarden" brand name in the app's translated strings.
// Re-run after every upstream merge: node arced/rebrand-strings.mjs
// Edits only the text of <string> and <item> lines, so names and formatting are untouched.
import { readdirSync, readFileSync, writeFileSync } from "node:fs";

const NAME = "ARCEDians Vault";
const res = new URL("../ui/src/main/res/", import.meta.url);

// Strings whose English text names a real Bitwarden product, page or service stay as they are.
const KEEP_TEXT =
  /Bitwarden (Authenticator|help center|Help center|Help Center|community|tools|Tools|mobile, browser)|bitwarden\.com|Bitwarden CSV|customer success|subscription|Bitwarden \(\.json\)|Get emails from Bitwarden|Log in at %1\$s/;

// A <string> or plural <item> line, with the resource name tracked from the enclosing tag.
const STRING = /^(\s*<string name="([^"]+)"[^>]*>)(.*)(<\/string>\s*)$/;
const PLURALS = /^\s*<plurals name="([^"]+)"/;
const ITEM = /^(\s*<item quantity="[^"]+">)(.*)(<\/item>\s*)$/;

const english = readFileSync(new URL("values/strings.xml", res), "utf8");
const keep = new Set();
let plural = null;
for (const line of english.split("\n")) {
  const p = line.match(PLURALS);
  if (p) plural = p[1];
  const s = line.match(STRING);
  if (s && KEEP_TEXT.test(s[3])) keep.add(s[2]);
  const i = line.match(ITEM);
  if (i && plural && KEEP_TEXT.test(i[2])) keep.add(plural);
}

let changed = 0;
for (const dir of readdirSync(res).filter((d) => d.startsWith("values"))) {
  const file = new URL(`${dir}/strings.xml`, res);
  let text;
  try {
    text = readFileSync(file, "utf8");
  } catch {
    continue;
  }
  let plural = null;
  const out = text.split("\n").map((line) => {
    const p = line.match(PLURALS);
    if (p) plural = p[1];
    const s = line.match(STRING);
    const i = !s && line.match(ITEM);
    const name = s ? s[2] : i ? plural : null;
    if (!name || keep.has(name)) return line;
    const [open, body, close] = s ? [s[1], s[3], s[4]] : [i[1], i[2], i[3]];
    const next = body.replace(/\bBitwarden\b(?! Authenticator)/g, NAME);
    if (next === body) return line;
    changed++;
    return open + next + close;
  });
  writeFileSync(file, out.join("\n"));
}
console.log(`updated ${changed} strings, kept ${keep.size} Bitwarden product strings`);
