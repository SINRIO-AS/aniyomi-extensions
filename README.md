# Rule34Video Aniyomi Extension Fix

هذا المشروع يعيد بناء **إضافة Aniyomi Rule34Video** من APK المرفق، وليس تطبيقاً مستقلاً. يحافظ الناتج على package الإضافة الأصلي `eu.kanade.tachiyomi.animeextension.en.rule34video` وعلى اسمها `Aniyomi: Rule34Video`.

يقوم البناء بفك الحزمة الأصلية، واستبدال `parseDetails` في smali، ثم إعادة التجميع والتوقيع ورفع الملف `Rule34Video-v14.12-fixed.apk` كـartifact.

## الإصلاح

تقوم الإضافة بعد الإصلاح بقراءة العنوان والوصف والصورة، والـmodels أو المؤلف، وروابط tags وcategories، وروابط groups أو albums أو collections عند وجودها. وبما أن نموذج Aniyomi لا يملك حقلاً مستقلاً للمجموعة أو رابط الصفحة، تُضاف هذه البيانات إلى الوصف، بينما تُحفظ الوسوم والتصنيفات في حقل Genre. كما أن رابط الفيديو الأصلي يبقى في عنوان الإدخال/الرابط الخاص بالإضافة.

## GitHub Actions

شغّل workflow باسم **Build Rule34Video Aniyomi Extension**. الناتج الصحيح هو `Rule34Video-Aniyomi-extension-fixed/Rule34Video-v14.12-fixed.apk`. لا تستخدم APK التطبيق المستقل السابق ذي package `space.manus.rule34video.android.v2`.
