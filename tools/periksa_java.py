#!/usr/bin/env python3
"""Pemeriksa awal berkas .java sebelum dikirim ke perakit GitHub.

Menangkap kesalahan yang PASTI menggagalkan `javac -source 8`:
  1. escape tidak sah di dalam teks berkutip ("\\s", "\\d", "\\.") -- ini yang
     paling sering terjadi saat menulis pola regex
  2. text block (\"\"\") yang tidak didukung -source 8
  3. lambda (->) yang butuh java.lang.invoke
  4. kurung/kurawal tidak seimbang (setelah string & komentar dibuang)
  5. impor kembar
  6. nama kelas tidak cocok nama berkas

Pakai:  python3 periksa_java.py <folder sumber>
"""
import os
import re
import sys

ESCAPE_SAH = set('btnfr"\'\\')


def buang_string_dan_komentar(t):
    """Ganti isi string/komentar jadi kosong, sisakan struktur kode."""
    keluar = []
    i = 0
    n = len(t)
    while i < n:
        c = t[i]
        if c == '/' and i + 1 < n and t[i + 1] == '/':
            while i < n and t[i] != '\n':
                i += 1
        elif c == '/' and i + 1 < n and t[i + 1] == '*':
            i += 2
            while i + 1 < n and not (t[i] == '*' and t[i + 1] == '/'):
                i += 1
            i += 2
        elif c == '"':
            i += 1
            while i < n:
                if t[i] == '\\':
                    i += 2
                    continue
                if t[i] == '"':
                    i += 1
                    break
                i += 1
            keluar.append('""')
        elif c == "'":
            i += 1
            while i < n:
                if t[i] == '\\':
                    i += 2
                    continue
                if t[i] == "'":
                    i += 1
                    break
                i += 1
            keluar.append("''")
        else:
            keluar.append(c)
            i += 1
    return ''.join(keluar)


def periksa_string(t):
    """Periksa escape di dalam setiap teks berkutip. Kembalikan daftar masalah."""
    masalah = []
    i = 0
    n = len(t)
    baris = 1
    while i < n:
        c = t[i]
        if c == '\n':
            baris += 1
            i += 1
        elif c == '/' and i + 1 < n and t[i + 1] == '/':
            while i < n and t[i] != '\n':
                i += 1
        elif c == '/' and i + 1 < n and t[i + 1] == '*':
            i += 2
            while i + 1 < n and not (t[i] == '*' and t[i + 1] == '/'):
                if t[i] == '\n':
                    baris += 1
                i += 1
            i += 2
        elif c == '"':
            if t[i:i + 3] == '"""':
                masalah.append((baris, 'text block (""" ) tidak didukung -source 8'))
                i += 3
                continue
            i += 1
            while i < n and t[i] != '"':
                if t[i] == '\\':
                    if i + 1 >= n:
                        masalah.append((baris, 'backslash di akhir berkas'))
                        break
                    nx = t[i + 1]
                    if nx not in ESCAPE_SAH and not nx.isdigit() and nx != 'u':
                        masalah.append((baris, 'escape tidak sah: \\%s' % nx))
                    i += 2
                    continue
                if t[i] == '\n':
                    masalah.append((baris, 'teks berkutip tidak ditutup'))
                    break
                i += 1
            i += 1
        else:
            i += 1
    return masalah


def periksa_berkas(path):
    t = open(path, encoding='utf-8', errors='replace').read()
    nama = os.path.basename(path)
    masalah = []
    for baris, pesan in periksa_string(t):
        masalah.append('%s:%d: %s' % (nama, baris, pesan))
    if '->' in buang_string_dan_komentar(t):
        masalah.append('%s: ada lambda (->) -- dilarang untuk -source 8' % nama)
    bersih = buang_string_dan_komentar(t)
    for buka, tutup in [('{', '}'), ('(', ')')]:
        if bersih.count(buka) != bersih.count(tutup):
            masalah.append('%s: %s %d vs %s %d (tidak seimbang)'
                           % (nama, buka, bersih.count(buka), tutup, bersih.count(tutup)))
    impor = re.findall(r'^import\s+([\w.]+);', t, re.M)
    kembar = sorted({x for x in impor if impor.count(x) > 1})
    if kembar:
        masalah.append('%s: impor kembar %s' % (nama, ', '.join(kembar)))
    kelas = re.findall(r'\bclass\s+(\w+)', t)
    if nama.endswith('.java') and nama[:-5] not in kelas:
        masalah.append('%s: nama berkas tidak cocok dengan kelas %s' % (nama, kelas))
    if not t.lstrip().startswith('package '):
        masalah.append('%s: tidak ada baris package' % nama)
    return masalah


def main():
    akar = sys.argv[1] if len(sys.argv) > 1 else 'src'
    berkas = []
    for dirpath, _, nama in os.walk(akar):
        for n in nama:
            if n.endswith('.java'):
                berkas.append(os.path.join(dirpath, n))
    semua = []
    for p in sorted(berkas):
        semua.extend(periksa_berkas(p))
    if semua:
        print('DITEMUKAN %d MASALAH:' % len(semua))
        for m in semua:
            print('  ' + m)
        sys.exit(1)
    print('BERSIH: %d berkas java lolos pemeriksaan awal' % len(berkas))


if __name__ == '__main__':
    main()
