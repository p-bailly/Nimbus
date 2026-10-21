# Ajoute à CMU Serif les symboles mathématiques qui lui manquent (ℤ, ∈, ⟶, ∫…), pris dans Latin Modern Math,
# pour que les formules des accueils « papier » soient composées dans une seule famille Computer Modern.
# Usage : python3 fusion.py cmu_serif_roman.ttf papier_regular.ttf … (fontTools ; latinmodern-math.otf du CTAN à côté)
import sys
from fontTools.ttLib import TTFont
from fontTools.pens.ttGlyphPen import TTGlyphPen
from fontTools.pens.cu2quPen import Cu2QuPen
from fontTools.pens.transformPen import TransformPen
NEEDED = "ℤℂℝℕℚℙℒ⟶⟼⟹⇒↦→←↔∈∉∋∪∩∖⊂⊃⊆⊇≅≃≤≥≠≈≡∫∮∑∏∘∙⋅∞∂∇∀∃∅□∎⊕⊗⌊⌋⌈⌉⟨⟩‖∣∥⁴⁵⁶⁷⁸⁹′″∗√∝"
src = TTFont('latinmodern-math.otf'); scmap = src.getBestCmap(); sgs = src.getGlyphSet()
for inp, out in zip(sys.argv[1::2], sys.argv[2::2]):
    f = TTFont(inp); cmap = f.getBestCmap()
    scale = f['head'].unitsPerEm / src['head'].unitsPerEm
    glyf, hmtx = f['glyf'], f['hmtx']
    order = f.getGlyphOrder()
    added = []
    for ch in NEEDED:
        cp = ord(ch)
        if cp in cmap or cp not in scmap: continue
        sname = scmap[cp]; name = 'lmm_' + sname
        pen = TTGlyphPen(None)
        sgs[sname].draw(Cu2QuPen(TransformPen(pen, (scale, 0, 0, scale, 0, 0)), max_err=1.0, reverse_direction=True))
        g = pen.glyph(); glyf[name] = g
        adv, lsb = src['hmtx'][sname]
        g.recalcBounds(glyf)
        hmtx[name] = (round(adv * scale), getattr(g, 'xMin', 0))
        for t in f['cmap'].tables:
            if t.isUnicode(): t.cmap[cp] = name
        added.append(ch)
    order = list(glyf.glyphOrder)
    assert len(order) == len(glyf.glyphs), (len(order), len(glyf.glyphs))
    f.setGlyphOrder(order)
    f.save(out)
    print(out, 'added', len(added), ''.join(added))
