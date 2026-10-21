package fr.perso.nimbus.ui.unlock

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import fr.perso.nimbus.ui.theme.PaperColors
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

// Figures du thème « Espace » : les cinq solides de Platon, la stella octangula, l'hypercube, et des
// surfaces (tore, ruban de Möbius, selle de cheval, nœud de trèfle), le ballon de football, la sphère
// géodésique et la double hélice. Un petit moteur 3D les tourne, les
// projette en perspective légère et les dessine de la face la plus lointaine à la plus proche (algorithme
// du peintre) ; chaque face est éclairée selon son orientation : plus pâle et peu hachurée face à la
// lumière, plus appuyée dans l'ombre.

private class Vec3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)
    operator fun times(s: Float) = Vec3(x * s, y * s, z * s)
    infix fun dot(o: Vec3) = x * o.x + y * o.y + z * o.z
    infix fun cross(o: Vec3) = Vec3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x)
    fun norm() = sqrt(this dot this)
    fun unit() = this * (1f / max(norm(), 1e-6f))
}

private fun vec3(x: Number, y: Number, z: Number) = Vec3(x.toFloat(), y.toFloat(), z.toFloat())

/** Une face à dessiner : ses sommets (dans l'espace) et sa couleur. */
private class SolidFace(val pts: List<Vec3>, val color: Color)

/** Le nombre d'or. */
private val PHI = ((1 + sqrt(5.0)) / 2).toFloat()

/** Lumière : en haut à gauche, devant. */
private val LIGHT = vec3(-0.45, 0.6, 0.66).unit()

/** Distance de l'œil, pour la perspective. */
private const val EYE = 7f

/**
 * Le point de vue : rotation de [yaw] degrés autour de l'axe vertical, puis de [pitch] autour de l'axe
 * horizontal, puis de [roll] dans le plan de la page.
 */
private class SolidView(yaw: Float, pitch: Float, roll: Float = 0f) {
    private val cy = cos(rad(yaw)); private val sy = sin(rad(yaw))
    private val cp = cos(rad(pitch)); private val sp = sin(rad(pitch))
    private val cr = cos(rad(roll)); private val sr = sin(rad(roll))

    fun turn(p: Vec3): Vec3 {
        val x1 = p.x * cy + p.z * sy
        val z1 = -p.x * sy + p.z * cy
        val y2 = p.y * cp - z1 * sp
        val z2 = p.y * sp + z1 * cp
        return Vec3(x1 * cr - y2 * sr, x1 * sr + y2 * cr, z2)
    }

    /** Projection en perspective (z vers l'œil), y vers le haut de la page. */
    fun project(p: Vec3): Offset {
        val s = EYE / (EYE - p.z)
        return Offset(p.x * s, -p.y * s)
    }
}

/**
 * Dessine [faces] vues de [view] : faces tournées, triées de la plus lointaine à la plus proche, puis
 * coloriées sur un fond de papier (elles cachent ce qui est derrière). [cull] : solide convexe fermé,
 * les faces de dos ne sont pas dessinées. [hatchArea] : aire (en unités²) au-dessus de laquelle une
 * face est hachurée ; [lw] : épaisseur du contour.
 */
private fun Pencil.render(
    faces: List<SolidFace>,
    view: SolidView,
    cull: Boolean,
    hatchArea: Float = 600f,
    lw: Float = 1.2f,
    margin: Float = 12f,
) {
    class DrawnFace(val pts: List<Offset>, val depth: Float, val light: Float, val color: Color)
    val drawn = mutableListOf<DrawnFace>()
    for (f in faces) {
        val t = f.pts.map { view.turn(it) }
        var n = (t[1] - t[0]) cross (t[2] - t[0])
        if (n.norm() < 1e-7f && t.size > 3) n = (t[2] - t[0]) cross (t[3] - t[0])
        n = n.unit()
        if (cull && n.z <= 0.01f) continue
        val light = if (cull) n dot LIGHT else abs(n dot LIGHT)
        drawn += DrawnFace(t.map { view.project(it) }, t.sumOf { it.z.toDouble() }.toFloat() / t.size, light, f.color)
    }
    val fit = Fit(drawn.map { it.pts }, margin)
    for ((i, d) in drawn.sortedBy { it.depth }.withIndex()) {
        val pts = fit(d.pts)
        // Éclairement de 0 (ombre) à 1 (face à la lumière).
        val b = ((d.light + 1f) / 2f).coerceIn(0f, 1f)
        val color = lerp(d.color, Graphite, 0.28f * (1f - b))
        scope.drawPath(path(pts), PaperColors.Paper)
        val area = area(pts)
        fill(
            pts,
            color,
            hatch = area > hatchArea,
            angle = -58f + (i % 5) * 23f,
            gap = 2.6f + 3.2f * b,
            wash = 0.62f - 0.4f * b,
            lw = lw,
            strokeAlpha = 0.85f - 0.25f * b,
        )
    }
}

private fun area(pts: List<Offset>): Float {
    var s = 0f
    for (i in pts.indices) {
        val a = pts[i]; val b = pts[(i + 1) % pts.size]
        s += a.x * b.y - b.x * a.y
    }
    return abs(s) / 2
}

/**
 * Faces d'un polyèdre régulier : pour chaque direction de [normals], les sommets les plus avancés dans
 * cette direction forment une face, rangés en tournant autour d'elle ; couleur selon [color].
 */
private fun regularFaces(vertices: List<Vec3>, normals: List<Vec3>, color: (Int) -> Color): List<SolidFace> =
    normals.mapIndexed { i, raw ->
        val n = raw.unit()
        val top = vertices.maxOf { it dot n }
        val on = vertices.filter { abs((it dot n) - top) < 1e-3f }
        val c = on.reduce { a, b -> a + b } * (1f / on.size)
        val u = (on[0] - c).unit()
        val w = n cross u
        SolidFace(on.sortedBy { val p = it - c; atan2(p dot w, p dot u) }, color(i))
    }

/** Toutes les combinaisons de signes de [x], [y], [z] (les nuls restent nuls), sans doublon. */
private fun signs(x: Float, y: Float, z: Float): List<Vec3> {
    val out = mutableListOf<Vec3>()
    for (sx in if (x == 0f) listOf(1f) else listOf(1f, -1f))
        for (sy in if (y == 0f) listOf(1f) else listOf(1f, -1f))
            for (sz in if (z == 0f) listOf(1f) else listOf(1f, -1f)) out += Vec3(x * sx, y * sy, z * sz)
    return out
}

/** Les trois permutations circulaires de (x, y, z), avec tous les signes. */
private fun cyclic(x: Float, y: Float, z: Float): List<Vec3> = signs(x, y, z) + signs(z, x, y) + signs(y, z, x)

private val CUBE_VERTS = signs(1f, 1f, 1f)
private val OCTA_VERTS = cyclic(1f, 0f, 0f)
private val ICOSA_VERTS = cyclic(0f, 1f, PHI)
private val DODECA_VERTS = signs(1f, 1f, 1f) + cyclic(0f, 1 / PHI, PHI)

/** Directions des faces : celles du dodécaèdre (vers les sommets de l'icosaèdre dual) et de l'icosaèdre. */
private val DODECA_NORMALS = cyclic(0f, PHI, 1f)
private val ICOSA_NORMALS = signs(1f, 1f, 1f) + cyclic(0f, PHI, 1 / PHI)

/** Rang de la paire {n, −n} à laquelle appartient la direction [i] de [normals]. */
private fun pairIndex(normals: List<Vec3>, i: Int): Int {
    val pairs = mutableListOf<Int>()
    for ((k, n) in normals.withIndex()) {
        val opposite = normals.indexOfFirst { (it + n).norm() < 1e-4f }
        if (opposite > k) pairs += k
    }
    val opposite = normals.indexOfFirst { (it + normals[i]).norm() < 1e-4f }
    return pairs.indexOf(minOf(i, opposite))
}

/** Point de vue d'un solide selon la variante. */
private fun solidView(v: Int, yaw: Float, pitch: Float) = SolidView(yaw + 23f * (v % 4), pitch + 7f * (v % 3), -6f + 4f * (v % 4))

private fun Pencil.tetrahedron(v: Int) {
    val verts = listOf(vec3(1, 1, 1), vec3(1, -1, -1), vec3(-1, 1, -1), vec3(-1, -1, 1))
    val pal = listOf(Crayons.rouge, Crayons.jaune, Crayons.bleu, Crayons.vert).rot(v % 4)
    render(regularFaces(verts, verts.map { it * -1f }) { pal[it] }, SolidView(130f + 12f * (v % 3), 30f, -4f + 5f * (v % 2)), cull = true, margin = 14f)
}

private fun Pencil.cube(v: Int) {
    val pal = listOf(Crayons.orange, Crayons.turquoise, Crayons.violet, Crayons.rose, Crayons.vert, Crayons.bleu).rot(v % 6)
    // Faces opposées de même couleur.
    render(regularFaces(CUBE_VERTS, OCTA_VERTS) { pal[it / 2] }, solidView(v, 35f, 24f), cull = true, margin = 18f)
}

private fun Pencil.octahedron(v: Int) {
    val a = listOf(Crayons.bleu, Crayons.rose, Crayons.orange, Crayons.vert)[v % 4]
    val b = listOf(Crayons.jaune, Crayons.turquoise, Crayons.violet, Crayons.rouge)[v % 4]
    // Deux couleurs alternées : deux faces qui se touchent par une arête n'ont jamais la même.
    render(regularFaces(OCTA_VERTS, CUBE_VERTS) { i -> val n = CUBE_VERTS[i]; if (n.x * n.y * n.z > 0) a else b }, solidView(v, 28f, 18f), cull = true, margin = 12f)
}

private fun Pencil.dodecahedron(v: Int) {
    val pal = listOf(Crayons.rouge, Crayons.orange, Crayons.jaune, Crayons.vert, Crayons.bleu, Crayons.violet).rot(v % 6)
    // Faces opposées de même couleur : les faces visibles ont toutes une couleur différente.
    val faces = regularFaces(DODECA_VERTS, DODECA_NORMALS) { i -> pal[pairIndex(DODECA_NORMALS, i) % 6] }
    render(faces, solidView(v, 20f, 16f), cull = true, margin = 12f)
}

private fun Pencil.icosahedron(v: Int) {
    val pal = listOf(Crayons.turquoise, Crayons.bleu, Crayons.violet, Crayons.rose, Crayons.jaune).rot(v % 5)
    val faces = regularFaces(ICOSA_VERTS, ICOSA_NORMALS) { i -> pal[pairIndex(ICOSA_NORMALS, i) % 5] }
    render(faces, solidView(v, 18f, 14f), cull = true, hatchArea = 350f, margin = 12f)
}

private fun Pencil.stella(v: Int) {
    val a = listOf(Crayons.jaune, Crayons.rouge, Crayons.turquoise, Crayons.orange)[v % 4]
    val b = listOf(Crayons.bleu, Crayons.vert, Crayons.violet, Crayons.marine)[v % 4]
    val faces = mutableListOf<SolidFace>()
    // Chaque coin du cube porte une pointe posée sur une face de l'octaèdre commun.
    for (c in CUBE_VERTS) {
        val base = listOf(vec3(c.x, 0, 0), vec3(0, c.y, 0), vec3(0, 0, c.z))
        val color = if (c.x * c.y * c.z > 0) a else b
        for (i in 0 until 3) {
            var tri = listOf(c, base[i], base[(i + 1) % 3])
            val n = (tri[1] - tri[0]) cross (tri[2] - tri[0])
            val g = (tri[0] + tri[1] + tri[2]) * (1f / 3)
            if ((n dot g) < 0) tri = tri.reversed()
            faces += SolidFace(tri, color)
        }
    }
    render(faces, SolidView(48f + 90f * (v % 4), 27f, -2f + 4f * (v % 2)), cull = true, hatchArea = 300f, lw = 1.1f, margin = 10f)
}

private fun Pencil.tesseract(v: Int) {
    // Sommets (±1, ±1, ±1, ±1), à peine tournés dans le plan xw, puis projetés de la dimension 4 vers 3.
    val a = rad(6f + 8f * (v % 3)); val bAngle = 0f
    val verts = (0 until 16).map { i ->
        val c = FloatArray(4) { d -> if ((i shr d) and 1 == 1) 1f else -1f }
        val x = c[0] * cos(a) - c[3] * sin(a)
        val w = c[0] * sin(a) + c[3] * cos(a)
        val y = c[1] * cos(bAngle) - c[2] * sin(bAngle)
        val z = c[1] * sin(bAngle) + c[2] * cos(bAngle)
        val s = 3f / (3f - w)
        vec3(x * s, y * s, z * s)
    }
    val view = SolidView(28f + 12f * (v % 3), 22f)
    val proj = verts.map { view.project(view.turn(it)) }
    val fit = Fit(listOf(proj), 16f)
    val p = proj.map { fit(it) }
    // Le cube intérieur (w = −1 avant rotation : le plus petit) en lavis.
    val inner = (0 until 16).filter { (it shr 3) and 1 == 0 }
    val cubeFaces = listOf(listOf(0, 1, 3, 2), listOf(4, 5, 7, 6), listOf(0, 1, 5, 4), listOf(2, 3, 7, 6), listOf(0, 2, 6, 4), listOf(1, 3, 7, 5))
    for (f in cubeFaces) fill(f.map { p[inner[it]] }, Crayons.jaune, hatch = false, wash = 0.16f, outline = false)
    val pal = listOf(Crayons.rouge, Crayons.bleu, Crayons.vert, Crayons.violet).rot(v % 4)
    // Les 32 arêtes : deux sommets qui ne diffèrent que d'une coordonnée ; couleur selon la direction.
    for (i in 0 until 16) for (d in 0 until 4) {
        val j = i xor (1 shl d)
        if (j > i) line(listOf(p[i], p[j]), pal[d], 2f)
    }
    for (q in p) dot(q, 2.6f, Graphite, 0.9f)
}

/** Surface paramétrée découpée en [nu] × [nv] quadrilatères, couleur selon les indices. */
private fun surface(nu: Int, nv: Int, closedU: Boolean, closedV: Boolean, at: (Int, Int) -> Vec3, color: (Int, Int) -> Color): List<SolidFace> {
    val faces = mutableListOf<SolidFace>()
    val iu = if (closedU) nu else nu - 1
    val iv = if (closedV) nv else nv - 1
    for (i in 0 until iu) for (j in 0 until iv) {
        val i1 = (i + 1) % nu; val j1 = (j + 1) % nv
        faces += SolidFace(listOf(at(i, j), at(i1, j), at(i1, j1), at(i, j1)), color(i, j))
    }
    return faces
}

private fun Pencil.torus(v: Int) {
    val nu = 40; val nv = 16; val big = 1f; val small = 0.42f
    val pal = Crayons.rainbow.rot(v % 8)
    val stripes = v % 2 == 0
    val faces = surface(nu, nv, true, true, { i, j ->
        val u = 2 * PI.toFloat() * i / nu; val w = 2 * PI.toFloat() * j / nv
        vec3((big + small * cos(w)) * cos(u), small * sin(w), (big + small * cos(w)) * sin(u))
    }) { i, j -> if (stripes) pal[i * 8 / nu] else pal[j * 8 / nv] }
    render(faces, SolidView(15f + 20f * (v % 3), 34f), cull = true, hatchArea = 140f, lw = 0.55f, margin = 10f)
}

private fun Pencil.mobius(v: Int) {
    val nu = 72; val nv = 6
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    // nu + 1 rangées ouvertes : à u = 2π, la paramétrisation retourne d'elle-même la bande (w devient −w).
    val faces = surface(nu + 1, nv, false, false, { i, j ->
        val u = 2 * PI.toFloat() * i / nu
        val w = -0.45f + 0.9f * j / (nv - 1)
        vec3((1 + w * cos(u / 2)) * cos(u), w * sin(u / 2), (1 + w * cos(u / 2)) * sin(u))
    }) { i, _ -> pal[min(7, i * 8 / nu)] }
    render(faces, SolidView(10f + 25f * (v % 3), 38f), cull = false, hatchArea = 140f, lw = 0.6f, margin = 10f)
}

private fun Pencil.saddle(v: Int) {
    val n = 16
    val pal = listOf(Crayons.marine, Crayons.bleu, Crayons.turquoise, Crayons.vert, Crayons.jaune, Crayons.orange, Crayons.rouge)
    val faces = surface(n + 1, n + 1, false, false, { i, j ->
        val x = -1f + 2f * i / n; val y = -1f + 2f * j / n
        vec3(x, (x * x - y * y) * 0.8f, y)
    }) { i, j ->
        val x = -1f + 2f * (i + 0.5f) / n; val y = -1f + 2f * (j + 0.5f) / n
        val z = (x * x - y * y + 1f) / 2f
        val c = pal[(z * (pal.size - 1)).toInt().coerceIn(0, pal.size - 1)]
        if (v % 2 == 1) pal[pal.size - 1 - pal.indexOf(c)] else c
    }
    render(faces, SolidView(24f + 10f * (v % 2), 30f), cull = false, hatchArea = 160f, lw = 0.6f, margin = 10f)
}

private fun Pencil.trefoil(v: Int) {
    val nu = 180; val nv = 10; val tube = 0.34f
    fun curve(t: Float) = vec3(sin(t) + 2 * sin(2 * t), cos(t) - 2 * cos(2 * t), -sin(3 * t))
    val pal = Crayons.rainbow.rot(v % 8)
    val faces = surface(nu, nv, true, true, { i, j ->
        val t = 2 * PI.toFloat() * i / nu
        val c = curve(t)
        // Repère de Frenet : la normale principale suit la courbure (jamais nulle sur ce nœud).
        val h = 0.01f
        val tangent = (curve(t + h) - curve(t - h)).unit()
        val bend = curve(t + h) + curve(t - h) - c * 2f
        val n = (bend - tangent * (bend dot tangent)).unit()
        val b = tangent cross n
        val w = 2 * PI.toFloat() * j / nv
        c + n * (tube * cos(w)) + b * (tube * sin(w))
    }) { i, _ -> pal[i * 8 / nu] }
    render(faces, SolidView(8f * (v % 3), 12f + 10f * (v % 2)), cull = false, hatchArea = 120f, lw = 0.5f, margin = 10f)
}

/** Ballon de football : icosaèdre tronqué, 12 pentagones foncés et 20 hexagones. */
private fun Pencil.soccerBall(v: Int) {
    // Sommets : permutations paires (circulaires) de (0, ±1, ±3φ), (±1, ±(2 + φ), ±2φ), (±φ, ±2, ±(2φ + 1)).
    // Rayon ramené à 1,7 (celui des autres solides) : à rayon 5, l'œil serait presque dessus.
    val raw = cyclic(0f, 1f, 3 * PHI) + cyclic(1f, 2 + PHI, 2 * PHI) + cyclic(PHI, 2f, 2 * PHI + 1)
    val verts = raw.map { it * (1.7f / raw[0].norm()) }
    val dark = Color(0xFF3B4150)
    val pale = Color(0xFFE9E2D0)
    val pal = Crayons.rainbow.rot(v % 8)
    // Pentagones : vers les sommets de l'icosaèdre ; hexagones : vers le centre de ses faces.
    val pentagons = regularFaces(verts, ICOSA_VERTS) { dark }
    val hexagons = regularFaces(verts, ICOSA_NORMALS) { i -> if (v % 2 == 0) pale else pal[pairIndex(ICOSA_NORMALS, i) % 8] }
    render(pentagons + hexagons, solidView(v, 22f, 18f), cull = true, hatchArea = 260f, lw = 1f, margin = 12f)
}

/** Sphère géodésique : chaque face de l'icosaèdre découpée en 16 triangles, ramenés sur la sphère. */
private fun Pencil.geodesic(v: Int) {
    val f = 4
    val base = regularFaces(ICOSA_VERTS, ICOSA_NORMALS) { Crayons.bleu }
    val bands = listOf(Crayons.marine, Crayons.bleu, Crayons.turquoise, Crayons.vert, Crayons.jaune, Crayons.orange, Crayons.rouge)
    val pal = Crayons.rainbow.rot(v % 8)
    val faces = mutableListOf<SolidFace>()
    for ((k, tri) in base.withIndex()) {
        val (a, b, c) = tri.pts
        fun at(i: Int, j: Int) = (a + (b - a) * (i.toFloat() / f) + (c - a) * (j.toFloat() / f)).unit()
        fun add(p: List<Vec3>) {
            val g = (p[0] + p[1] + p[2]).unit()
            // Variante paire : bandes de latitude ; impaire : la couleur de la face de l'icosaèdre d'origine.
            val color = if (v % 2 == 0) bands[((g.y + 1f) / 2f * bands.size).toInt().coerceIn(0, bands.size - 1)] else pal[pairIndex(ICOSA_NORMALS, k) % 8]
            faces += SolidFace(p, color)
        }
        for (i in 0 until f) for (j in 0 until f - i) {
            add(listOf(at(i, j), at(i + 1, j), at(i, j + 1)))
            if (i + j < f - 1) add(listOf(at(i + 1, j), at(i + 1, j + 1), at(i, j + 1)))
        }
    }
    render(faces, solidView(v, 14f, 20f), cull = true, hatchArea = 230f, lw = 0.75f, margin = 10f)
}

/** Double hélice : deux rubans qui s'enroulent autour d'un même axe, reliés par des barreaux. */
private fun Pencil.doubleHelix(v: Int) {
    val turns = 2.5f
    val pitch = 0.36f
    val r = 1f
    val width = 0.13f
    val tMax = 2 * PI.toFloat() * turns
    // Axe horizontal (x) ; le second brin décalé d'un peu plus d'un demi-tour, comme le grand et le petit sillon de l'ADN.
    val offset = PI.toFloat() * 0.8f
    fun strand(t: Float, phase: Float) = vec3(t * pitch - tMax * pitch / 2, r * cos(t + phase), r * sin(t + phase))
    val axis = vec3(1, 0, 0)
    val strandColors = listOf(Crayons.marine to Crayons.rouge, Crayons.violet to Crayons.orange, Crayons.sapin to Crayons.rose)[v % 3]
    val faces = mutableListOf<SolidFace>()
    val n = 150
    for ((phase, color) in listOf(0f to strandColors.first, offset to strandColors.second)) {
        for (i in 0 until n) {
            val t0 = tMax * i / n; val t1 = tMax * (i + 1) / n
            val p0 = strand(t0, phase); val p1 = strand(t1, phase)
            faces += SolidFace(listOf(p0 - axis * width, p1 - axis * width, p1 + axis * width, p0 + axis * width), color)
        }
    }
    // Barreaux : dix par tour, chacun en deux moitiés de couleurs complémentaires (A–T, G–C).
    val pairs = listOf(Crayons.jaune to Crayons.vert, Crayons.turquoise to Crayons.rose, Crayons.vert to Crayons.jaune, Crayons.rose to Crayons.turquoise)
    val rungs = (turns * 10).toInt()
    for (k in 0 until rungs) {
        val t = tMax * (k + 0.5f) / rungs
        val a = strand(t, 0f); val b = strand(t, offset)
        val m = (a + b) * 0.5f
        val (ca, cb) = pairs[(k * 7 + v) % pairs.size]
        val w = width * 0.55f
        faces += SolidFace(listOf(a - axis * w, m - axis * w, m + axis * w, a + axis * w), ca)
        faces += SolidFace(listOf(m - axis * w, b - axis * w, b + axis * w, m + axis * w), cb)
    }
    render(faces, SolidView(-18f + 12f * (v % 3), 16f, -14f + 6f * (v % 2)), cull = false, hatchArea = 10_000f, lw = 0.6f, margin = 10f)
}

internal val spaceFigures: List<FigureSpec> = listOf(
    FigureSpec("tetrahedron", "Tétraèdre", "Espace", { _ -> "" }) { tetrahedron(it) },
    FigureSpec("cube", "Cube", "Espace", { _ -> "" }) { cube(it) },
    FigureSpec("octahedron", "Octaèdre", "Espace", { _ -> "" }) { octahedron(it) },
    FigureSpec("dodecahedron", "Dodécaèdre", "Espace", { _ -> "" }) { dodecahedron(it) },
    FigureSpec("icosahedron", "Icosaèdre", "Espace", { _ -> "" }) { icosahedron(it) },
    FigureSpec("stella", "Stella octangula", "Espace", { _ -> "" }) { stella(it) },
    FigureSpec("tesseract", "Hypercube", "Espace", { _ -> "" }) { tesseract(it) },
    FigureSpec("torus", "Tore", "Espace", { _ -> "" }) { torus(it) },
    FigureSpec("mobius", "Ruban de Möbius", "Espace", { _ -> "" }) { mobius(it) },
    FigureSpec("saddle", "Selle de cheval", "Espace", { _ -> "" }) { saddle(it) },
    FigureSpec("trefoil", "Nœud de trèfle", "Espace", { _ -> "" }) { trefoil(it) },
    FigureSpec("soccer_ball", "Ballon de football", "Espace", { _ -> "" }) { soccerBall(it) },
    FigureSpec("geodesic", "Sphère géodésique", "Espace", { _ -> "" }) { geodesic(it) },
    FigureSpec("double_helix", "Double hélice", "Espace", { _ -> "" }) { doubleHelix(it) },
)
