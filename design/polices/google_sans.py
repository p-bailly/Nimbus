"""Google Sans Flex (licence SIL OFL 1.1, sans nom réservé) pour l'écran verrouillé en fond d'écran.

Police de l'horloge du Pixel : la page la reprend à côté de l'heure du système. Fichier d'origine :
https://github.com/google/fonts/tree/main/ofl/googlesansflex (GoogleSansFlex[ROND,opsz,slnt,wdth,wght].ttf).
On ne garde que le latin, et on fixe ROND = 0, slnt = 0 et wdth = 100 : il reste les axes wght et opsz.

    pip install fonttools
    python3 design/polices/google_sans.py GoogleSansFlex[ROND,opsz,slnt,wdth,wght].ttf
"""
import sys

from fontTools import subset
from fontTools.ttLib import TTFont
from fontTools.varLib import instancer

font = TTFont(sys.argv[1])
options = subset.Options()
options.layout_features = ["*"]
options.name_IDs = ["*"]
options.notdef_outline = True
sub = subset.Subsetter(options)
# Latin de base et Latin-1, ligatures latines, ponctuation, °, ‘’“”«», €, espaces fines.
sub.populate(unicodes=[*range(0x20, 0x7F), *range(0xA0, 0x180), *range(0x2000, 0x2070), 0x20AC, 0xFB01, 0xFB02])
sub.subset(font)
font = instancer.instantiateVariableFont(font, {"ROND": 0, "slnt": 0, "wdth": 100})
font.save("app/src/main/res/font/google_sans_flex.ttf")
