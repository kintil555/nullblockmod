# Null Block (Forge 1.21.4)

Block yang selalu bisa ditembus player (no collision), dan bisa "menyamar" sebagai block lain
secara visual dengan klik kanan menggunakan block item (misal Stone).

## Cara pakai in-game
1. Pasang Null Block seperti block biasa.
2. Klik kanan block Null Block dengan block lain di tangan (misal Stone) → Null Block akan
   tampil seperti Stone, tapi player tetap bisa jalan tembus.
3. Shift + klik kanan tangan kosong pada Null Block yang sudah punya disguise → disguise
   dihapus (kembali invisible).

Bekerja untuk **semua block** yang terdaftar di game (vanilla maupun dari mod lain), karena
disguise disimpan sebagai `BlockState` generik di block entity, bukan hardcoded per-block.

## Struktur

- `block/NullBlock.java` — block utama: collision selalu kosong (`Shapes.empty()`), render
  dasar `INVISIBLE`, interaksi klik-kanan untuk set/hapus disguise.
- `block/entity/NullBlockEntity.java` — menyimpan `BlockState` disguise per-posisi, sinkron
  client-server, tersimpan ke NBT.
- `block/entity/NullBlockEntityRenderer.java` — menggambar ulang model block disguise di posisi
  Null Block (visual only, tidak memengaruhi collision).
- `api/NullBlockAPI.java` — **API publik untuk mod lain**: taruh block, ganti disguise, cek
  status null block, dan `makePassable(level, pos)` untuk mengubah block yang sudah ada jadi
  passable dengan tampilan yang sama.

## Menjadikan mod ini dependensi

Mod lain tinggal `compileOnly`/`implementation` jar ini lalu memanggil method statis di
`com.nullblock.mod.api.NullBlockAPI`, contoh:

```java
// taruh null block yang disguise sebagai Stone
NullBlockAPI.placeNullBlock(level, pos, Blocks.STONE, true);

// ubah block yang sudah ada jadi passable, tampilan tetap sama
NullBlockAPI.makePassable(level, pos);

// generate banyak null block acak saat world generation (contoh use-case saja,
// BUKAN fitur bawaan mod ini — mod lain yang mengimplementasikan logic acaknya)
for (BlockPos p : someRandomPositions) {
    NullBlockAPI.placeNullBlock(level, p, Blocks.STONE, true);
}
```

Tambahkan dependency di `mods.toml` mod kamu:
```toml
[[dependencies.yourmodid]]
    modId = "nullblock"
    mandatory = true
    versionRange = "[1.0,)"
    ordering = "AFTER"
    side = "BOTH"
```

## Build

File gradle (`build.gradle`, `settings.gradle`, `gradle.properties`) sudah disertakan sebagai
starting point untuk Forge MDK 1.21.4 (Forge `1.21.4-54.1.0`). Sesuaikan sendiri sesuai
kebutuhanmu (mapping channel, ForgeGradle version, dsb) — silakan cek ulang di
https://files.minecraftforge.net/net/minecraftforge/forge/index_1.21.4.html untuk versi terbaru.

Build standar:
```
./gradlew build
```
Output ada di `build/libs/nullblock-1.0.0.jar`.
