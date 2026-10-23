package fr.perso.nimbus.ui.unlock

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import fr.perso.nimbus.ui.theme.PaperColors
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

// Figures du thème « Espace » : les cinq solides de Platon, la stella octangula, l'hypercube, et des
// surfaces (tore, ruban de Möbius, selle de cheval, nœud de trèfle), la sphère géodésique. Deuxième
// série : la fibration de Hopf, un nœud torique, les dodécaèdres étoilés de Kepler-Poinsot, le
// rhombicosidodécaèdre, des loxodromies, la bouteille de Klein, l'hyperboloïde en fils tendus,
// l'hélicoïde, un coquillage, l'empilement de Kepler et le triangle impossible de Penrose.
// Un petit moteur 3D les tourne, les projette en perspective légère et les dessine de la face la plus
// lointaine à la plus proche (algorithme du peintre) ; chaque face est éclairée selon son orientation :
// plus pâle et peu hachurée face à la lumière, plus appuyée dans l'ombre.

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

/**
 * Une face à dessiner : ses sommets (dans l'espace) et sa couleur. Les faces d'un même [group] (une pointe
 * d'étoile) sont dessinées ensemble, à la profondeur moyenne du groupe.
 */
private class SolidFace(val pts: List<Vec3>, val color: Color, val group: Int = -1)

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
    class DrawnFace(val pts: List<Offset>, val depth: Float, val light: Float, val color: Color, val groupDepth: Float)
    val groupDepths = faces.filter { it.group >= 0 }.groupBy { it.group }.mapValues { (_, g) ->
        g.flatMap { f -> f.pts }.map { view.turn(it).z }.average().toFloat()
    }
    val drawn = mutableListOf<DrawnFace>()
    for (f in faces) {
        val t = f.pts.map { view.turn(it) }
        var n = (t[1] - t[0]) cross (t[2] - t[0])
        if (n.norm() < 1e-7f && t.size > 3) n = (t[2] - t[0]) cross (t[3] - t[0])
        n = n.unit()
        if (cull && n.z <= 0.01f) continue
        val light = if (cull) n dot LIGHT else abs(n dot LIGHT)
        val depth = t.sumOf { it.z.toDouble() }.toFloat() / t.size
        drawn += DrawnFace(t.map { view.project(it) }, depth, light, f.color, groupDepths[f.group] ?: depth)
    }
    val fit = Fit(drawn.map { it.pts }, margin)
    for ((i, d) in drawn.sortedWith(compareBy({ it.groupDepth }, { it.depth })).withIndex()) {
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

internal val spaceFigures: List<FigureSpec> = listOf(
    FigureSpec("tetrahedron", "Tétraèdre", "Espace",
    { _ ->
        "Le plus simple des cinq solides de Platon : 4 faces qui sont des triangles équilatéraux, 6 arêtes et 4 sommets, où se rejoignent 3 faces. " +
            "La relation d’Euler S − A + F = 2 s’écrit 4 − 6 + 4 = 2. Il est son propre dual : en reliant les centres de ses faces, on obtient un autre tétraèdre."
    }) { tetrahedron(it) },
    FigureSpec("cube", "Cube", "Espace",
    { _ ->
        "Le cube a 6 faces carrées, 12 arêtes et 8 sommets où se rejoignent 3 faces : 8 − 12 + 6 = 2 (relation d’Euler). " +
            "Ici, deux faces opposées ont la même couleur. En reliant les centres de ses faces, on obtient son dual, l’octaèdre, qui a autant d’arêtes et échange faces et sommets."
    }) { cube(it) },
    FigureSpec("octahedron", "Octaèdre", "Espace",
    { _ ->
        "L’octaèdre a 8 faces triangulaires équilatérales, 12 arêtes et 6 sommets où se rejoignent 4 faces : 6 − 12 + 8 = 2. C’est le dual du cube : faces et sommets s’échangent. " +
            "Deux couleurs suffisent pour que deux faces qui se touchent par une arête soient toujours de couleurs différentes."
    }) { octahedron(it) },
    FigureSpec("dodecahedron", "Dodécaèdre", "Espace",
    { _ ->
        "Le dodécaèdre a 12 faces pentagonales régulières, 30 arêtes et 20 sommets où se rejoignent 3 faces : 20 − 30 + 12 = 2. " +
            "Ses mesures font intervenir le nombre d’or φ ≈ 1,618 : ses sommets sont, entre autres, aux points (0, ±1/φ, ±φ). Son dual est l’icosaèdre."
    }) { dodecahedron(it) },
    FigureSpec("icosahedron", "Icosaèdre", "Espace",
    { _ ->
        "L’icosaèdre a 20 faces triangulaires équilatérales, 30 arêtes et 12 sommets où se rejoignent 5 faces : 12 − 30 + 20 = 2. " +
            "Ses sommets sont aux points (0, ±1, ±φ) et leurs permutations circulaires, avec φ le nombre d’or. C’est le dual du dodécaèdre, et le solide de Platon qui a le plus de faces."
    }) { icosahedron(it) },
    FigureSpec("stella", "Stella octangula", "Espace",
    { _ ->
        "Deux tétraèdres réguliers imbriqués, l’un retourné par rapport à l’autre : leurs 8 sommets sont ceux d’un cube, et leur partie commune est un octaèdre. " +
            "Kepler l’a baptisée stella octangula, « étoile à huit branches » : chaque branche est une petite pyramide posée sur une face de l’octaèdre."
    }) { stella(it) },
    FigureSpec("tesseract", "Hypercube", "Espace",
    { _ ->
        "L’hypercube est au cube ce que le cube est au carré : ses 16 sommets ont 4 coordonnées valant ±1. Deux sommets sont reliés quand ils ne diffèrent que d’une coordonnée : 32 arêtes, une couleur par direction. " +
            "Projeté de la dimension 4 vers 3, il apparaît comme un petit cube dans un grand, reliés coin à coin ; il a 24 faces carrées et 8 cubes."
    }) { tesseract(it) },
    FigureSpec("torus", "Tore", "Espace",
    { _ ->
        "Le tore est la surface d’une bouée : un cercle de rayon r qui tourne autour d’un axe, à une distance R de son centre. " +
            "Son aire vaut 4π²Rr et son volume 2π²Rr². On y trace deux sortes de cercles, les parallèles et les méridiens ; sa caractéristique d’Euler vaut 0, et non 2 comme pour une sphère."
    }) { torus(it) },
    FigureSpec("mobius", "Ruban de Möbius", "Espace",
    { _ ->
        "On prend une bande de papier, on la tourne d’un demi-tour et on colle ses deux bouts : le ruban de Möbius n’a qu’une seule face et un seul bord. " +
            "Une fourmi qui en fait le tour revient à son point de départ, mais de l’autre côté du papier. Coupé en son milieu, il ne se sépare pas en deux : il donne un seul ruban, deux fois plus long."
    }) { mobius(it) },
    FigureSpec("saddle", "Selle de cheval", "Espace",
    { _ ->
        "La selle de cheval est la surface z = x² − y² (paraboloïde hyperbolique) : le long d’un axe elle monte en parabole, le long de l’autre elle descend. " +
            "Au centre, le col n’est ni un sommet ni un creux. Elle est formée de deux familles de droites : on peut la construire avec des baguettes bien droites. Les couleurs suivent l’altitude."
    }) { saddle(it) },
    FigureSpec("trefoil", "Nœud de trèfle", "Espace",
    { _ ->
        "Le plus simple des vrais nœuds : la courbe (sin t + 2 sin 2t, cos t − 2 cos 2t, −sin 3t) se referme en se croisant trois fois. " +
            "Aucune déformation, sans couper le tube, ne le dénoue en un simple anneau. Il existe en deux versions, images l’une de l’autre dans un miroir, qu’on ne peut pas changer l’une en l’autre."
    }) { trefoil(it) },
    FigureSpec("geodesic", "Sphère géodésique", "Espace",
    { _ ->
        "On part d’un icosaèdre, on découpe chacune de ses 20 faces en 16 petits triangles, puis on pousse leurs sommets sur la sphère : 320 faces, 480 arêtes et 162 sommets, et 162 − 480 + 320 = 2. " +
            "Douze sommets ont 5 voisins, tous les autres 6. C’est le principe des dômes géodésiques de Buckminster Fuller : solides et légers."
    }) { geodesic(it) },
)

// ------------------------------------------------------------ Deuxième série

private val TWO_PI = (2 * PI).toFloat()

/** Oriente la face [pts] pour que sa normale s'éloigne du point [inside]. */
private fun facing(pts: List<Vec3>, inside: Vec3): List<Vec3> {
    val n = (pts[1] - pts[0]) cross (pts[2] - pts[0])
    val g = pts.reduce { a, b -> a + b } * (1f / pts.size)
    return if ((n dot (g - inside)) < 0) pts.reversed() else pts
}

/** Milieux des arêtes (de longueur [edge]) d'un polyèdre de sommets [verts]. */
private fun edgeMidpoints(verts: List<Vec3>, edge: Float): List<Vec3> {
    val out = mutableListOf<Vec3>()
    for (i in verts.indices) for (j in i + 1 until verts.size)
        if (abs((verts[i] - verts[j]).norm() - edge) < 1e-3f) out += (verts[i] + verts[j]) * 0.5f
    return out
}

/**
 * Petit rhombicosidodécaèdre : les 12 faces du dodécaèdre écartées du centre (chaque sommet repoussé le
 * long de la normale de sa face), de sorte que les vides deviennent des carrés et des triangles.
 */
private fun Pencil.rhombicosidodecahedron(v: Int) {
    val edge = 2f / PHI
    val push = edge / sqrt(2f - 2f / sqrt(5f))
    val verts = regularFaces(DODECA_VERTS, DODECA_NORMALS) { Graphite }.flatMapIndexed { i, f ->
        val n = DODECA_NORMALS[i].unit()
        f.pts.map { it + n * push }
    }
    val sets = listOf(
        listOf(Crayons.rouge, Crayons.orange, Crayons.jaune, Crayons.turquoise),
        listOf(Crayons.violet, Crayons.rose, Crayons.jaune, Crayons.vert),
        listOf(Crayons.marine, Crayons.bleu, Crayons.orange, Crayons.jaune),
        listOf(Crayons.sapin, Crayons.vert, Crayons.rose, Crayons.bleu),
    )
    val pal = sets[v % sets.size]
    val pentagons = Crayons.rainbow.filter { it != pal[2] && it != pal[3] }.rot(v % 5)
    val faces = regularFaces(verts, DODECA_NORMALS) { i -> pentagons[pairIndex(DODECA_NORMALS, i) % 6] } +
        regularFaces(verts, DODECA_VERTS) { pal[2] } +
        regularFaces(verts, edgeMidpoints(DODECA_VERTS, edge)) { pal[3] }
    render(faces, solidView(v, 16f, 18f), cull = true, hatchArea = 260f, lw = 1f, margin = 12f)
}

/**
 * Deux polyèdres de Kepler-Poinsot côte à côte, chacun tourné sur lui-même : à gauche le petit
 * dodécaèdre étoilé (une pyramide à base pentagonale sur chaque face d'un dodécaèdre), à droite le grand
 * dodécaèdre étoilé (une pyramide à base triangulaire sur chaque face d'un icosaèdre, de sommets ceux
 * d'un dodécaèdre).
 */
private fun Pencil.stellatedDodecahedra(v: Int) {
    val faces = mutableListOf<SolidFace>()
    val warm = listOf(Crayons.rouge, Crayons.orange, Crayons.jaune, Crayons.rose, Crayons.violet, Crayons.brun).rot(v % 6)
    val cool = listOf(Crayons.bleu, Crayons.turquoise, Crayons.vert, Crayons.marine, Crayons.violet).rot(v % 5)
    // Vus le long d'un axe d'ordre 5 (à gauche) et d'ordre 3 (à droite), un peu de biais.
    val viewA = SolidView(6f * (v % 3), 58.28f - 8f * (v % 2), 6f * (v % 4))
    val viewB = SolidView(-45f + 7f * (v % 3), 35.26f + 6f * (v % 2), -8f * (v % 2))
    fun place(view: SolidView, shift: Float, scale: Float, p: Vec3) = view.turn(p * scale) + vec3(shift, 0, 0)
    // Petit dodécaèdre étoilé : la pointe est là où se croisent les plans des cinq faces voisines.
    regularFaces(DODECA_VERTS, DODECA_NORMALS) { Graphite }.forEachIndexed { i, f ->
        val n = DODECA_NORMALS[i].unit()
        val apex = n * ((f.pts[0] dot n) * sqrt(5f))
        val base = f.pts.reduce { a, b -> a + b } * 0.2f
        val color = warm[pairIndex(DODECA_NORMALS, i) % 6]
        for (k in f.pts.indices) {
            val tri = facing(listOf(f.pts[k], f.pts[(k + 1) % 5], apex), base)
            faces += SolidFace(tri.map { place(viewA, -1.03f, 0.28f, it) }, color, i)
        }
    }
    // Grand dodécaèdre étoilé : icosaèdre intérieur et pointes aux sommets d'un dodécaèdre.
    val core = DODECA_NORMALS.map { it.unit() * 0.72654f }
    regularFaces(core, DODECA_VERTS) { Graphite }.forEachIndexed { i, f ->
        val apex = DODECA_VERTS[i]
        val base = f.pts.reduce { a, b -> a + b } * (1f / 3)
        val color = cool[pairIndex(DODECA_VERTS, i) % 5]
        for (k in f.pts.indices) {
            val tri = facing(listOf(f.pts[k], f.pts[(k + 1) % 3], apex), base)
            faces += SolidFace(tri.map { place(viewB, 1.03f, 0.5f, it) }, color, 100 + i)
        }
    }
    render(faces, SolidView(0f, 0f), cull = true, hatchArea = 200f, lw = 0.9f, margin = 12f)
}

/**
 * Fibration de Hopf : des cercles de la sphère S³, (cos η e^{it}, sin η e^{i(t + φ)}), projetés de S³ vers
 * l'espace par projection stéréographique. Pour un η donné, ils couvrent un tore ; chaque cercle est
 * dessiné comme un tube (trait de couleur bordé de graphite), morceau par morceau du plus lointain au
 * plus proche.
 */
private fun Pencil.hopf(v: Int) {
    val etas = if (v % 2 == 0) listOf(0.1f, 0.2f) else listOf(0.08f, 0.15f, 0.22f)
    val perTorus = if (v % 2 == 0) 10 else 8
    val n = 120
    val view = SolidView(10f + 20f * (v % 3), 62f + 6f * (v % 2), 0f)
    val pal = Crayons.rainbow.rot(v % 8)
    class Piece(val a: Vec3, val b: Vec3, val color: Color)
    val pieces = mutableListOf<Piece>()
    etas.forEachIndexed { e, eta ->
        val ce = cos(eta * PI.toFloat()); val se = sin(eta * PI.toFloat())
        for (f in 0 until perTorus) {
            val phase = TWO_PI * f / perTorus + e * 0.6f
            val color = pal[(f * 8 / perTorus + 3 * e) % 8]
            val ring = (0..n).map { i ->
                val t = TWO_PI * i / n
                val d = 1 - se * sin(t + phase)
                view.turn(vec3(ce * cos(t) / d, se * cos(t + phase) / d, ce * sin(t) / d) * 0.8f)
            }
            for (i in 0 until n) pieces += Piece(ring[i], ring[i + 1], color)
        }
    }
    val fit = Fit(pieces.map { listOf(view.project(it.a)) }, 16f)
    // Bord de graphite en bout droit, couleur en bout rond : les morceaux d'un même tube se raccordent sans couture.
    for (p in pieces.sortedBy { it.a.z + it.b.z }) {
        val a = px(fit(view.project(p.a))); val b = px(fit(view.project(p.b)))
        // Le bord est un peu prolongé à chaque bout : pas de jour entre deux morceaux.
        val dl = (b - a) * (0.3f * k / max((b - a).getDistance(), 1e-3f))
        val z = (p.a.z + p.b.z) / 2
        val w = 2.8f * EYE / (EYE - z)
        val light = (z / 1.5f).coerceIn(-1f, 1f)
        scope.drawLine(Graphite, a - dl, b + dl, strokeWidth = (w + 1.4f) * k, cap = StrokeCap.Butt, alpha = 0.9f)
        scope.drawLine(PaperColors.Paper, a, b, strokeWidth = w * k, cap = StrokeCap.Round)
        scope.drawLine(lerp(p.color, Graphite, 0.15f * (1 - light)), a, b, strokeWidth = w * k, cap = StrokeCap.Round, alpha = 0.6f + 0.15f * light)
    }
}

/**
 * Nœud torique (p, q) en tube : la courbe s'enroule p fois autour de l'axe du tore et q fois autour de
 * son âme.
 */
private fun Pencil.torusKnot(v: Int) {
    val (p, q) = TORUS_KNOTS[v % TORUS_KNOTS.size]
    val nu = 70 * p + 20; val nv = 8; val tube = 0.26f
    // Tore un peu aplati : la courbe ne plonge jamais à pic dans le trou.
    fun curve(t: Float) = vec3((2.3f + 0.8f * cos(q * t)) * cos(p * t), 0.45f * sin(q * t), (2.3f + 0.8f * cos(q * t)) * sin(p * t))
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    val faces = surface(nu, nv, true, true, { i, j ->
        val t = TWO_PI * i / nu
        val c = curve(t)
        val tangent = (curve(t + 0.01f) - curve(t - 0.01f)).unit()
        // Repère du tube : la normale du tore, redressée perpendiculairement à la courbe ; pas de torsion parasite.
        val nrm = vec3(cos(q * t) * cos(p * t), sin(q * t), cos(q * t) * sin(p * t)).let { (it - tangent * (it dot tangent)).unit() }
        val b = tangent cross nrm
        val w = TWO_PI * j / nv
        c + nrm * (tube * cos(w)) + b * (tube * sin(w))
    }) { i, _ -> pal[(i * 2 * q / nu) % 8] }
    render(faces, SolidView(10f * (v % 3), 52f - 6f * (v % 2), 0f), cull = false, hatchArea = 120f, lw = 0.5f, margin = 10f)
}

/** Les nœuds toriques (p, q) des variantes, p et q premiers entre eux. */
private val TORUS_KNOTS = listOf(2 to 5, 3 to 5, 2 to 7, 3 to 4)

/** Nombre minimal de croisements du nœud torique (p, q). */
private fun torusCrossings(p: Int, q: Int) = min(p * (q - 1), q * (p - 1))

/**
 * Loxodromies : des bandes de la sphère bordées par des courbes qui coupent tous les méridiens sous le
 * même angle (longitude = a − pente × ln tan(π/4 + latitude/2)) ; une bande sur deux est enlevée, et
 * l'on voit l'intérieur de la sphère par les vides, comme dans les Sphères en spirales d'Escher.
 */
private fun Pencil.loxodromes(v: Int) {
    val bands = LOXODROME_BANDS[v % LOXODROME_BANDS.size]
    val across = 5; val rows = 36; val slope = 1.05f
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    val faces = mutableListOf<SolidFace>()
    fun at(a: Float, lat: Float): Vec3 {
        val lon = a - slope * ln(kotlin.math.tan(PI.toFloat() / 4 + lat / 2))
        return vec3(cos(lat) * cos(lon), sin(lat), cos(lat) * sin(lon))
    }
    for (b in 0 until bands) for (i in 0 until across) for (j in 0 until rows) {
        val a0 = TWO_PI * (2 * b * across + i) / (2 * bands * across)
        val a1 = TWO_PI * (2 * b * across + i + 1) / (2 * bands * across)
        val l0 = rad(-74f + 148f * j / rows); val l1 = rad(-74f + 148f * (j + 1) / rows)
        val color = lerp(pal[(b * 8 / bands) % 8], Graphite, (j / 3 % 2) * 0.16f)
        faces += SolidFace(listOf(at(a0, l0), at(a1, l0), at(a1, l1), at(a0, l1)), color)
    }
    render(faces, solidView(v, 10f, 18f), cull = false, hatchArea = 90f, lw = 0.5f, margin = 12f)
}

/** Nombre de bandes de la sphère selon la variante. */
private val LOXODROME_BANDS = listOf(4, 3, 5)

/**
 * Bouteille de Klein dans son immersion classique (paramétrage de Wikipédia, en forme de bouteille) : le
 * goulot traverse la paroi et rejoint le fond par l'intérieur. La panse est dessinée en écorché.
 */
private fun Pencil.kleinBottle(v: Int) {
    val nu = 64; val nv = 18
    val pal = Crayons.rainbow.rot((v * 5) % 8)
    val faces = surface(nu + 1, nv, false, true, { i, j ->
        val u = PI.toFloat() * i / nu; val w = TWO_PI * j / nv
        val c = cos(u); val s = sin(u); val cw = cos(w)
        val c2 = c * c; val c4 = c2 * c2; val c6 = c4 * c2
        val x = -2f / 15 * c * (3 * cw - 30 * s + 90 * c4 * s - 60 * c6 * s + 5 * c * cw * s)
        val y = -1f / 15 * s * (3 * cw - 3 * c2 * cw - 48 * c4 * cw + 48 * c6 * cw - 60 * s + 5 * c * cw * s -
            5 * c2 * c * cw * s - 80 * c4 * c * cw * s + 80 * c6 * c * cw * s)
        val z = 2f / 15 * (3 + 5 * c * s) * sin(w)
        vec3(x - 0.15f, y - 2.1f, z)
    }) { i, _ -> pal[min(7, i * 8 / nu)] }
    // Écorché : on enlève la moitié avant de la panse (là où le tube est large) pour voir le goulot y entrer.
    val cut = faces.filterIndexed { k, _ ->
        val i = k / nv; val j = k % nv
        val u = PI.toFloat() * (i + 0.5f) / nu; val w = TWO_PI * (j + 0.5f) / nv
        !(3 + 5 * cos(u) * sin(u) > 4.6f && sin(w) > 0.05f)
    }
    render(cut, SolidView(-20f + 25f * (v % 3), 14f, -78f + 8f * (v % 2)), cull = false, hatchArea = 140f, lw = 0.5f, margin = 10f)
}

/**
 * Hyperboloïde à une nappe en fils tendus : deux familles de droites joignent les points de deux cercles
 * décalés de ±2β. Les fils de derrière sont pâles ; un léger lavis donne la surface.
 */
private fun Pencil.hyperboloid(v: Int) {
    val n = 30; val ring = 1.2f; val half = 1.2f
    val beta = rad(listOf(64f, 56f, 70f)[v % 3])
    val view = solidView(v, 8f, 20f)
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    val colors = listOf(pal[0], pal[4])
    val k2 = (ring * sin(beta) / half).let { it * it }
    fun at(a: Float, y: Float) = vec3(ring * cos(a), y, ring * sin(a))
    fun proj(p: Vec3) = view.project(view.turn(p))
    fun front(p: Vec3, nrm: Vec3): Boolean {
        val t = view.turn(p)
        return (view.turn(nrm) dot (vec3(0, 0, EYE) - t)) > 0
    }
    val rings = listOf(-half, half).map { y -> (0..72).map { at(TWO_PI * it / 72, y) } }
    val fit = Fit(rings.map { r -> r.map { proj(it) } }, 18f)
    // Lavis : les bandes entre deux fils voisins d'une même famille.
    for (f in 0 until n) {
        val a = TWO_PI * f / n; val b = TWO_PI * (f + 1) / n
        val quad = listOf(at(a - beta, -half), at(b - beta, -half), at(b + beta, half), at(a + beta, half)).map { fit(proj(it)) }
        fill(quad, pal[2], hatch = false, wash = 0.07f, outline = false)
    }
    class Piece(val a: Offset, val b: Offset, val front: Boolean, val color: Color)
    val pieces = mutableListOf<Piece>()
    for (family in 0..1) for (f in 0 until n) {
        val a = TWO_PI * (f + 0.5f * family) / n
        val sgn = if (family == 0) 1f else -1f
        val p0 = at(a - sgn * beta, -half); val p1 = at(a + sgn * beta, half)
        for (s in 0 until 14) {
            val q0 = p0 + (p1 - p0) * (s / 14f); val q1 = p0 + (p1 - p0) * ((s + 1) / 14f)
            val m = (q0 + q1) * 0.5f
            pieces += Piece(fit(proj(q0)), fit(proj(q1)), front(m, vec3(m.x, -k2 * m.y, m.z)), colors[family])
        }
    }
    // Les morceaux d'un même fil, mis bout à bout tant qu'ils restent du même côté.
    fun runs(front: Boolean): List<Pair<List<Offset>, Color>> {
        val out = mutableListOf<Pair<List<Offset>, Color>>()
        var cur = mutableListOf<Offset>()
        for ((i, p) in pieces.withIndex()) {
            if (i % 14 == 0 || p.front != front) {
                if (cur.size > 1) out += cur to pieces[i - 1].color
                cur = mutableListOf()
            }
            if (p.front == front) { if (cur.isEmpty()) cur += p.a; cur += p.b }
        }
        if (cur.size > 1) out += cur to pieces.last().color
        return out
    }
    for ((run, color) in runs(false)) line(run, color, 0.8f, 0.5f)
    fun hoop(r: List<Vec3>) {
        val pts = r.map { fit(proj(it)) }
        line(pts, Graphite, 3.4f)
        line(pts, Crayons.brun, 2f, 0.8f)
    }
    hoop(rings[0])
    for ((run, color) in runs(true)) line(run, color, 1.4f, 0.95f)
    hoop(rings[1])
    for (family in 0..1) for (f in 0 until n) {
        val a = TWO_PI * (f + 0.5f * family) / n
        val sgn = if (family == 0) 1f else -1f
        dot(fit(proj(at(a + sgn * beta, half))), 1.1f, Graphite, 0.8f)
    }
}

/** Hélicoïde : une droite qui avance le long d'un axe (ici couché) en tournant, en bandes de couleur. */
private fun Pencil.helicoid(v: Int) {
    val turns = if (v % 2 == 0) 2f else 1.5f
    val nu = (turns * 44).toInt(); val nv = 6
    val pal = Crayons.rainbow.rot(v % 8)
    val faces = surface(nu + 1, nv + 1, false, false, { i, j ->
        val t = -turns * PI.toFloat() + TWO_PI * turns * i / nu
        val s = -1f + 2f * j / nv
        vec3(0.62f * t / turns, s * cos(t), s * sin(t))
    }) { i, j -> lerp(pal[(i * 16 / nu) % 8], Graphite, if (j == 0 || j == nv - 1) 0.12f else 0f) }
    render(faces, SolidView(-28f + 14f * (v % 3), 16f, 4f - 4f * (v % 2)), cull = false, hatchArea = 45f, lw = 0.55f, margin = 12f)
}

/**
 * Coquillage : un tube dont toutes les mesures sont multipliées par [growth] à chaque tour, enroulé le
 * long d'une spirale logarithmique tracée sur un cône. Les tours successifs se touchent.
 */
private fun Pencil.seashell(v: Int) {
    val turret = v % 2 == 0
    val growth = SHELL_GROWTH
    val k = ln(growth) / TWO_PI
    val h = if (turret) 1.45f else 0.5f
    val rho = (growth - 1) * sqrt(1 + h * h) / (growth + 1) * 1.06f
    val turns = if (turret) 5f else 4f
    val nu = (turns * 28).toInt(); val nv = 12
    val pal = listOf(
        listOf(Crayons.orange, Crayons.jaune, Crayons.rouge, Crayons.jaune),
        listOf(Crayons.rose, Crayons.violet, Crayons.jaune, Crayons.violet),
        listOf(Crayons.turquoise, Crayons.jaune, Crayons.bleu, Crayons.jaune),
        listOf(Crayons.rouge, Crayons.rose, Crayons.orange, Crayons.rose),
    )[v % 4]
    val faces = surface(nu + 1, nv, false, true, { i, j ->
        val t = -TWO_PI * turns * (1 - i.toFloat() / nu)
        val g = exp(k * t)
        val s = TWO_PI * j / nv
        val radial = vec3(cos(t), 0, sin(t))
        radial * (g * (1 + rho * cos(s))) + vec3(0, g * (-h + rho * sin(s)) + h * 0.5f, 0)
    }) { i, j -> if (i % 7 == 0) Crayons.brun else pal[(j / 3) % 4] }
    render(faces, SolidView(20f + 30f * (v % 3), if (turret) 16f else 34f, if (turret) 0f else -8f), cull = false, hatchArea = 140f, lw = 0.5f, margin = 10f)
}

/** Facteur d'agrandissement du coquillage à chaque tour. */
private const val SHELL_GROWTH = 1.55f

/**
 * Empilement de Kepler : des boules qui se touchent, en pyramide à base carrée (4 × 4) ou triangulaire
 * (côté 5) selon la variante, dessinées de la plus lointaine à la plus proche, une couleur par étage.
 */
private fun Pencil.spherePacking(v: Int) {
    val square = v % 2 == 0
    val centers = mutableListOf<Pair<Vec3, Int>>()
    if (square) {
        for (k in 0 until 4) for (i in 0 until 4 - k) for (j in 0 until 4 - k)
            centers += vec3(i + k / 2f, k * sqrt(0.5f), j + k / 2f) to k
    } else {
        val h = sqrt(3f) / 2
        for (k in 0 until 5) for (i in 0 until 5 - k) for (j in 0 until 5 - k - i)
            centers += vec3(i + j / 2f + k / 2f, k * sqrt(2f / 3f), j * h + k * h / 3f) to k
    }
    val mean = centers.map { it.first }.reduce { a, b -> a + b } * (1f / centers.size)
    val scale = 0.62f
    val view = if (square) solidView(v, 24f, 22f) else SolidView(15f + 30f * (v % 4), 36f, -4f)
    val pal = listOf(Crayons.orange, Crayons.jaune, Crayons.rouge, Crayons.rose, Crayons.orange).let { if (square) it else Crayons.rainbow.rot((v * 3) % 8) }
    class Ball(val c: Offset, val r: Float, val depth: Float, val layer: Int)
    val balls = centers.map { (p, layer) ->
        val t = view.turn((p - mean) * scale)
        Ball(view.project(t), 0.5f * scale * EYE / (EYE - t.z), t.z, layer)
    }
    val fit = Fit(balls.map { circle(it.c.x, it.c.y, it.r, 24) }, 14f)
    for ((i, b) in balls.sortedBy { it.depth }.withIndex()) {
        val c = fit(b.c); val r = b.r * fit.s
        val disc = circle(c.x, c.y, r, 40)
        val color = pal[b.layer % pal.size]
        scope.drawPath(path(disc), PaperColors.Paper)
        fill(disc, color, angle = -58f + 31f * (i % 5), gap = 3.4f, wash = 0.4f, lw = 1.1f)
        // Ombre propre en bas à droite, reflet en haut à gauche.
        line((0..10).map { c + dir(rad(-20f + 13f * it)) * (r * 0.8f) }, lerp(color, Graphite, 0.35f), 1.6f, 0.55f)
        dot(c + Offset(-0.36f * r, -0.4f * r), 0.2f * r, PaperColors.Paper, 0.85f)
    }
}

/**
 * Triangle de Penrose : trois faces en L, images l'une de l'autre par un tiers de tour, dessinées sur
 * un réseau triangulaire (poutres d'épaisseur 1, triangle de côté n). Chaque coin est vraisemblable, pas
 * l'ensemble.
 */
private fun Pencil.penroseTriangle(v: Int) {
    val m = (4 + v % 3).toFloat()
    val face = listOf(pt(-1f, -1f), pt(m, -1f), pt(m, m - 1f), pt(m - 1f, m - 2f), pt(m - 1f, 0f), pt(-1f, 0f))
    val c = pt(2 * m / 3, m / 3)
    fun turn(p: Offset): Offset { val q = p - c; return c + Offset(-q.y, q.x - q.y) }
    val ex = dir(rad(30f)); val ey = dir(rad(150f))
    val mirror = if (v % 2 == 1) -1f else 1f
    fun screen(p: Offset): Offset { val s = ex * p.x + ey * p.y; return Offset(s.y * mirror, -s.x) }
    val faces = listOf(face, face.map { turn(it) }, face.map { turn(turn(it)) }).map { f -> f.map { screen(it) } }
    val fit = Fit(faces, 20f)
    val pal = Crayons.rainbow.rot(v % 8)
    val colors = listOf(pal[0], pal[3], pal[5])
    faces.forEachIndexed { i, f ->
        fill(fit(f), lerp(colors[i], Graphite, 0.1f * i), angle = listOf(-58f, 20f, 75f)[i], gap = listOf(5f, 3.6f, 2.6f)[i],
            wash = listOf(0.22f, 0.38f, 0.55f)[i], lw = 1.8f)
    }
}

/** Deuxième série du thème « Espace ». */
internal val moreSpaceFigures: List<FigureSpec> = listOf(
    FigureSpec("hopf", "Fibration de Hopf", "Espace",
    { v ->
        val tori = if (v % 2 == 0) 2 else 3
        val circles = if (v % 2 == 0) 10 else 8
        "La sphère de dimension 3, qui vit en dimension 4, se range en cercles disjoints, un par point d’une sphère ordinaire : c’est la fibration de Hopf (1931). " +
            "Projetés dans notre espace, ces cercles deviennent des cercles de Villarceau : deux d’entre eux sont toujours enlacés comme deux maillons. " +
            "Ici, $tori tores emboîtés de $circles cercles chacun."
    }) { hopf(it) },
    FigureSpec("torus_knot", "Nœud torique", "Espace",
    { v ->
        val (p, q) = TORUS_KNOTS[v % TORUS_KNOTS.size]
        "Un nœud torique (p, q) s’enroule p fois autour de l’axe d’un tore et q fois autour de son âme, sans jamais se couper. Ici p = $p et q = $q : " +
            "comme p et q sont premiers entre eux, la courbe se referme en un seul nœud, qui ne peut pas se dessiner avec moins de ${torusCrossings(p, q)} croisements. " +
            "Le nœud de trèfle est le nœud torique (2, 3)."
    }) { torusKnot(it) },
    FigureSpec("stellated_dodecahedra", "Dodécaèdres étoilés", "Espace",
    { _ ->
        "Deux des quatre polyèdres réguliers étoilés de Kepler-Poinsot. À gauche, le petit dodécaèdre étoilé : 12 faces en pentagramme, 30 arêtes, 12 sommets, " +
            "et 12 − 30 + 12 = −6 : la relation d’Euler ne vaut plus 2 quand les faces se traversent. À droite, le grand dodécaèdre étoilé : 12 pentagrammes, 30 arêtes " +
            "et 20 sommets, ceux d’un dodécaèdre."
    }) { stellatedDodecahedra(it) },
    FigureSpec("loxodromes", "Loxodromies", "Espace",
    { v ->
        val bands = LOXODROME_BANDS[v % LOXODROME_BANDS.size]
        "Une loxodromie coupe tous les méridiens sous le même angle : c’est la route d’un navire qui garde le même cap. Elle tourne sans fin autour du pôle " +
            "sans jamais l’atteindre, et pourtant sa longueur est finie. Sur une carte de Mercator, c’est une droite ; vue depuis le pôle, une spirale " +
            "logarithmique. Ici $bands bandes, comme dans les Sphères en spirales d’Escher (1958)."
    }) { loxodromes(it) },
    FigureSpec("klein_bottle", "Bouteille de Klein", "Espace",
    { _ ->
        "La bouteille de Klein (1882) est une surface fermée sans intérieur ni extérieur : comme le ruban de Möbius, elle n’a qu’une face. " +
            "Dans notre espace, son goulot doit traverser la paroi pour rejoindre le fond par l’intérieur (ici, la panse est ouverte pour le montrer) ; en dimension 4, il passerait à côté. " +
            "Sa caractéristique d’Euler vaut 0, comme celle du tore, et coupée en deux, elle donne deux rubans de Möbius."
    }) { kleinBottle(it) },
    FigureSpec("hyperboloid", "Hyperboloïde à une nappe", "Espace",
    { _ ->
        "L’hyperboloïde à une nappe, x² + z² − y² = 1 à l’échelle près, est une surface courbe faite de droites : par chacun de ses points passent deux droites " +
            "tracées sur la surface, une de chaque famille (ici deux couleurs). On le construit avec des fils tendus entre deux cercles tournés l’un par rapport " +
            "à l’autre ; c’est la forme des tours de refroidissement."
    }) { hyperboloid(it) },
    FigureSpec("helicoid", "Hélicoïde", "Espace",
    { v ->
        val turns = if (v % 2 == 0) "deux tours" else "un tour et demi"
        "Une droite horizontale monte le long d’un axe en tournant à vitesse constante, ici sur $turns : elle balaie l’hélicoïde, un escalier en colimaçon lisse. " +
            "C’est une surface minimale, comme un film de savon (Meusnier, 1776), et, avec le plan, la seule surface minimale réglée (Catalan, 1842). " +
            "On peut l’enrouler en caténoïde sans l’étirer."
    }) { helicoid(it) },
    FigureSpec("seashell", "Coquillage", "Espace",
    { v ->
        val shape = if (v % 2 == 0) "une tourelle pointue" else "un escargot plus aplati"
        "Un tube qui grandit en s’enroulant : à chaque tour, toutes ses mesures sont multipliées par 1,55, et son axe suit une spirale logarithmique tracée sur un cône. " +
            "Chaque tour a la forme du précédent en plus grand, comme les loges du nautile. Selon la pente du cône, on obtient $shape : " +
            "presque toutes les coquilles suivent cette règle."
    }) { seashell(it) },
    FigureSpec("sphere_packing", "Empilement de Kepler", "Espace",
    { v ->
        val pile = if (v % 2 == 0) "pyramide à base carrée : 16 + 9 + 4 + 1 = 30 boules" else "pyramide à base triangulaire : 15 + 10 + 6 + 3 + 1 = 35 boules"
        "La pile d’oranges du marchand, ici une $pile. Kepler affirma en 1611 qu’aucun empilement de boules identiques n’est plus dense : " +
            "elles occupent π/√18 ≈ 74 % de l’espace. Thomas Hales l’a démontré en 1998, et sa preuve a été vérifiée par ordinateur en 2014."
    }) { spherePacking(it) },
    FigureSpec("rhombicosidodecahedron", "Rhombicosidodécaèdre", "Espace",
    { _ ->
        "Un des 13 solides d’Archimède : 20 triangles, 30 carrés et 12 pentagones réguliers, soit 62 faces, 120 arêtes et 60 sommets, et 60 − 120 + 62 = 2. " +
            "Autour de chaque sommet : triangle, carré, pentagone, carré (3.4.5.4). On l’obtient en écartant les 12 faces d’un dodécaèdre ; les vides se comblent " +
            "de carrés et de triangles."
    }) { rhombicosidodecahedron(it) },
    FigureSpec("penrose_triangle", "Triangle de Penrose", "Espace",
    { _ ->
        "Trois poutres à section carrée, assemblées deux à deux à angle droit, forment pourtant un triangle. Chaque coin est vraisemblable ; c’est l’ensemble qui " +
            "est impossible : en suivant les poutres, on s’éloigne sans cesse de l’œil et l’on revient au départ. Dessiné par Oscar Reutersvärd en 1934, puis par " +
            "Lionel et Roger Penrose en 1958 ; Escher en a tiré sa Chute d’eau."
    }) { penroseTriangle(it) },
)
