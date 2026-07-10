#!/bin/bash
SRC="/tmp/iranian_bank_logos_vector/xml"
DST="app/src/main/res/drawable"

# Remove all current bank logo PNGs/XMLs
rm -f $DST/ic_bank_*.png
rm -f $DST/ic_bank_*.xml

cat << 'INNEREOF' > $DST/ic_bank_placeholder.xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24.0"
    android:viewportHeight="24.0">
    <path
        android:fillColor="#FF000000"
        android:pathData="M11.5,1L2,6v2h19V6L11.5,1zM4.14,6.5L11.5,2.68L18.86,6.5H4.14zM2,22v-2h19v2H2zM4,10v7h3v-7H4zM10,10v7h3v-7H10zM16,10v7h3v-7H16z"/>
</vector>
INNEREOF

cp "$SRC/melli_bank.xml" "$DST/ic_bank_melli.xml" 2>/dev/null || cp "$DST/ic_bank_placeholder.xml" "$DST/ic_bank_melli.xml"
cp "$SRC/mellat_bank.xml" "$DST/ic_bank_mellat.xml" 2>/dev/null || cp "$DST/ic_bank_placeholder.xml" "$DST/ic_bank_mellat.xml"
cp "$SRC/saman_bank.xml" "$DST/ic_bank_saman.xml" 2>/dev/null || cp "$DST/ic_bank_placeholder.xml" "$DST/ic_bank_saman.xml"
cp "$SRC/parsian_bank.xml" "$DST/ic_bank_parsian.xml" 2>/dev/null || cp "$DST/ic_bank_placeholder.xml" "$DST/ic_bank_parsian.xml"
cp "$SRC/saderat_bank.xml" "$DST/ic_bank_saderat.xml" 2>/dev/null || cp "$DST/ic_bank_placeholder.xml" "$DST/ic_bank_saderat.xml"
cp "$SRC/tejarat_bank.xml" "$DST/ic_bank_tejarat.xml" 2>/dev/null || cp "$DST/ic_bank_placeholder.xml" "$DST/ic_bank_tejarat.xml"
cp "$SRC/pasargad_bank.xml" "$DST/ic_bank_pasargad.xml" 2>/dev/null || cp "$DST/ic_bank_placeholder.xml" "$DST/ic_bank_pasargad.xml"
cp "$SRC/karafarin_bank.xml" "$DST/ic_bank_karafarin.xml" 2>/dev/null || cp "$DST/ic_bank_placeholder.xml" "$DST/ic_bank_karafarin.xml"
cp "$SRC/keshavarzi_bank.xml" "$DST/ic_bank_keshavarzi.xml" 2>/dev/null || cp "$DST/ic_bank_placeholder.xml" "$DST/ic_bank_keshavarzi.xml"
cp "$SRC/shahr_bank.xml" "$DST/ic_bank_shahr.xml" 2>/dev/null || cp "$DST/ic_bank_placeholder.xml" "$DST/ic_bank_shahr.xml"
cp "$SRC/iran_zamin_bank.xml" "$DST/ic_bank_iran_zamin.xml" 2>/dev/null || cp "$DST/ic_bank_placeholder.xml" "$DST/ic_bank_iran_zamin.xml"
cp "$SRC/eqtesade_novin_bank.xml" "$DST/ic_bank_eghtesad_novin.xml" 2>/dev/null || cp "$DST/ic_bank_placeholder.xml" "$DST/ic_bank_eghtesad_novin.xml"
cp "$SRC/day_bank.xml" "$DST/ic_bank_dey.xml" 2>/dev/null || cp "$DST/ic_bank_placeholder.xml" "$DST/ic_bank_dey.xml"
cp "$SRC/gardeshgari_bank.xml" "$DST/ic_bank_tourism.xml" 2>/dev/null || cp "$DST/ic_bank_placeholder.xml" "$DST/ic_bank_tourism.xml"
cp "$SRC/tosee_taavon_bank.xml" "$DST/ic_bank_tosee_taavon.xml" 2>/dev/null || cp "$DST/ic_bank_placeholder.xml" "$DST/ic_bank_tosee_taavon.xml"
cp "$SRC/sarmaye_bank.xml" "$DST/ic_bank_sarmayeh.xml" 2>/dev/null || cp "$DST/ic_bank_placeholder.xml" "$DST/ic_bank_sarmayeh.xml"
cp "$SRC/sanat_o_madan_bank.xml" "$DST/ic_bank_industry_and_mine.xml" 2>/dev/null || cp "$DST/ic_bank_placeholder.xml" "$DST/ic_bank_industry_and_mine.xml"
cp "$SRC/refah_kargaran_bank.xml" "$DST/ic_bank_refah.xml" 2>/dev/null || cp "$DST/ic_bank_placeholder.xml" "$DST/ic_bank_refah.xml"
cp "$SRC/mehr_bank.xml" "$DST/ic_bank_mehr_iran.xml" 2>/dev/null || cp "$DST/ic_bank_placeholder.xml" "$DST/ic_bank_mehr_iran.xml"

# Banks that might not be in the vector repo:
cp "$DST/ic_bank_placeholder.xml" "$DST/ic_bank_ayandeh.xml"
cp "$DST/ic_bank_placeholder.xml" "$DST/ic_bank_export_development.xml"
cp "$DST/ic_bank_placeholder.xml" "$DST/ic_bank_middle_east.xml"
cp "$DST/ic_bank_placeholder.xml" "$DST/ic_bank_sepah.xml"
cp "$DST/ic_bank_placeholder.xml" "$DST/ic_bank_sina.xml"
cp "$DST/ic_bank_placeholder.xml" "$DST/ic_bank_resalat.xml"
cp "$DST/ic_bank_placeholder.xml" "$DST/ic_bank_post_bank.xml"
cp "$DST/ic_bank_placeholder.xml" "$DST/ic_bank_melal.xml"
cp "$DST/ic_bank_placeholder.xml" "$DST/ic_bank_noor.xml"
cp "$DST/ic_bank_placeholder.xml" "$DST/ic_bank_maskan.xml"

rm "$DST/ic_bank_placeholder.xml"

