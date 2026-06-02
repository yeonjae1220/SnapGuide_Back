export const SUPPORTED_UI_LANGUAGES = ['ko', 'en', 'ja', 'zh'] as const
export type UiLanguage = (typeof SUPPORTED_UI_LANGUAGES)[number]

type MessageKey =
  | 'nav.explore'
  | 'nav.myGuides'
  | 'nav.upload'
  | 'nav.profile'
  | 'nav.login'
  | 'nav.logout'
  | 'landing.title'
  | 'landing.tagline'
  | 'landing.feat1.title'
  | 'landing.feat1.desc'
  | 'landing.feat2.title'
  | 'landing.feat2.desc'
  | 'landing.feat3.title'
  | 'landing.feat3.desc'
  | 'auth.login'
  | 'auth.signup'
  | 'auth.email'
  | 'auth.password'
  | 'auth.loginWithGoogle'
  | 'auth.or'
  | 'auth.pwdLen'
  | 'auth.pwdUpper'
  | 'auth.pwdLower'
  | 'auth.pwdDigit'
  | 'guide.views'
  | 'guide.tip'
  | 'guide.location'
  | 'guide.locationPrivate'
  | 'guide.noLocation'
  | 'guide.noExif'
  | 'guide.edit'
  | 'guide.delete'
  | 'guide.editPrompt'
  | 'guide.deleteConfirm'
  | 'guide.likeLoginRequired'
  | 'guide.empty'
  | 'upload.title'
  | 'upload.submit'
  | 'upload.tipLabel'
  | 'upload.tipPlaceholder'
  | 'upload.locationToggle'
  | 'upload.locationOn'
  | 'upload.locationOff'
  | 'upload.fileHint'
  | 'profile.email'
  | 'profile.noEmail'
  | 'profile.deleteAccount'
  | 'profile.deleteConfirm'
  | 'explore.radiusLabel'
  | 'explore.myLocation'
  | 'explore.searchPlaceholder'
  | 'explore.locateFailed'
  | 'explore.zoomInHint'
  | 'common.loading'
  | 'common.save'
  | 'common.cancel'
  | 'common.error'
  | 'settings.uiLanguage'
  | 'settings.appearance'
  | 'settings.themeLight'
  | 'settings.themeDark'

export type MessageKeyType = MessageKey

type Messages = Record<MessageKey, string>

export const messages: Record<UiLanguage, Messages> = {
  ko: {
    'nav.explore': '탐색',
    'nav.myGuides': '가이드',
    'nav.upload': '업로드',
    'nav.profile': '내 정보',
    'nav.login': '로그인',
    'nav.logout': '로그아웃',
    'landing.title': 'SnapGuide',
    'landing.tagline': '여행의 순간을 가이드로,\n세상과 나누다',
    'landing.feat1.title': '위치 기반 가이드',
    'landing.feat1.desc': '내 주변의 촬영 명소를 발견하세요',
    'landing.feat2.title': '촬영 팁 공유',
    'landing.feat2.desc': '최적의 앵글과 설정을 나눠보세요',
    'landing.feat3.title': '카메라 EXIF',
    'landing.feat3.desc': '실제 촬영 정보를 함께 기록하세요',
    'auth.login': '로그인',
    'auth.signup': '회원가입',
    'auth.email': '이메일',
    'auth.password': '비밀번호',
    'auth.loginWithGoogle': 'Google로 계속하기',
    'auth.or': '또는',
    'auth.pwdLen': '8자 이상',
    'auth.pwdUpper': '대문자 포함',
    'auth.pwdLower': '소문자 포함',
    'auth.pwdDigit': '숫자 포함',
    'guide.views': '조회',
    'guide.tip': '촬영 팁',
    'guide.location': '위치',
    'guide.locationPrivate': '위치 비공개',
    'guide.noLocation': '위치 정보 없음',
    'guide.noExif': '촬영 정보 없음',
    'guide.edit': '수정',
    'guide.delete': '삭제',
    'guide.editPrompt': '새 팁 내용을 입력하세요:',
    'guide.deleteConfirm': '정말 삭제하시겠습니까?',
    'guide.likeLoginRequired': '좋아요를 누르려면 로그인이 필요합니다.',
    'guide.empty': '근처에 가이드가 없습니다.',
    'upload.title': '가이드 업로드',
    'upload.submit': '업로드',
    'upload.tipLabel': '촬영 팁',
    'upload.tipPlaceholder': '이 장소의 촬영 팁을 입력하세요',
    'upload.locationToggle': '위치 공개',
    'upload.locationOn': '공개',
    'upload.locationOff': '비공개',
    'upload.fileHint': '사진 또는 영상을 선택하세요',
    'profile.email': '이메일',
    'profile.noEmail': '이메일 정보 없음',
    'profile.deleteAccount': '계정 삭제',
    'profile.deleteConfirm': '정말 계정을 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.',
    'explore.radiusLabel': '반경',
    'explore.myLocation': '내 위치',
    'explore.searchPlaceholder': '장소 검색',
    'explore.locateFailed': '현재 위치를 찾을 수 없습니다',
    'explore.zoomInHint': '지도를 확대하면 이 지역의 가이드를 볼 수 있습니다',
    'common.loading': '로딩 중...',
    'common.save': '저장',
    'common.cancel': '취소',
    'common.error': '오류가 발생했습니다.',
    'settings.uiLanguage': '언어',
    'settings.appearance': '화면 모드',
    'settings.themeLight': '라이트',
    'settings.themeDark': '다크',
  },
  en: {
    'nav.explore': 'Explore',
    'nav.myGuides': 'Guides',
    'nav.upload': 'Upload',
    'nav.profile': 'Profile',
    'nav.login': 'Log in',
    'nav.logout': 'Log out',
    'landing.title': 'SnapGuide',
    'landing.tagline': 'Turn your travel moments\ninto guides for the world',
    'landing.feat1.title': 'Location-Based Guides',
    'landing.feat1.desc': 'Discover shooting spots near you',
    'landing.feat2.title': 'Share Shooting Tips',
    'landing.feat2.desc': 'Share the best angles and settings',
    'landing.feat3.title': 'Camera EXIF',
    'landing.feat3.desc': 'Record real shooting data with your guide',
    'auth.login': 'Log in',
    'auth.signup': 'Sign up',
    'auth.email': 'Email',
    'auth.password': 'Password',
    'auth.loginWithGoogle': 'Continue with Google',
    'auth.or': 'or',
    'auth.pwdLen': 'At least 8 characters',
    'auth.pwdUpper': 'One uppercase letter',
    'auth.pwdLower': 'One lowercase letter',
    'auth.pwdDigit': 'One number',
    'guide.views': 'views',
    'guide.tip': 'Tip',
    'guide.location': 'Location',
    'guide.locationPrivate': 'Private location',
    'guide.noLocation': 'No location',
    'guide.noExif': 'No camera info',
    'guide.edit': 'Edit',
    'guide.delete': 'Delete',
    'guide.editPrompt': 'Enter new tip:',
    'guide.deleteConfirm': 'Delete this guide?',
    'guide.likeLoginRequired': 'Log in to like guides.',
    'guide.empty': 'No guides nearby.',
    'upload.title': 'Upload Guide',
    'upload.submit': 'Upload',
    'upload.tipLabel': 'Shooting tip',
    'upload.tipPlaceholder': 'Enter a tip for this spot',
    'upload.locationToggle': 'Share location',
    'upload.locationOn': 'Public',
    'upload.locationOff': 'Private',
    'upload.fileHint': 'Select a photo or video',
    'profile.email': 'Email',
    'profile.noEmail': 'No email on record',
    'profile.deleteAccount': 'Delete account',
    'profile.deleteConfirm': 'Delete your account? This cannot be undone.',
    'explore.radiusLabel': 'Radius',
    'explore.myLocation': 'My location',
    'explore.searchPlaceholder': 'Search places',
    'explore.locateFailed': 'Could not find your location',
    'explore.zoomInHint': 'Zoom in to see guides in this area',
    'common.loading': 'Loading…',
    'common.save': 'Save',
    'common.cancel': 'Cancel',
    'common.error': 'Something went wrong.',
    'settings.uiLanguage': 'Language',
    'settings.appearance': 'Appearance',
    'settings.themeLight': 'Light',
    'settings.themeDark': 'Dark',
  },
  ja: {
    'nav.explore': '探索',
    'nav.myGuides': 'ガイド',
    'nav.upload': 'アップロード',
    'nav.profile': 'プロフィール',
    'nav.login': 'ログイン',
    'nav.logout': 'ログアウト',
    'landing.title': 'SnapGuide',
    'landing.tagline': '旅の瞬間をガイドに、\n世界と分かち合う',
    'landing.feat1.title': '位置情報ガイド',
    'landing.feat1.desc': '近くの撮影スポットを見つけよう',
    'landing.feat2.title': '撮影テクニックを共有',
    'landing.feat2.desc': '最高のアングルと設定をシェアしよう',
    'landing.feat3.title': 'カメラEXIF',
    'landing.feat3.desc': '実際の撮影データも記録しよう',
    'auth.login': 'ログイン',
    'auth.signup': '新規登録',
    'auth.email': 'メールアドレス',
    'auth.password': 'パスワード',
    'auth.loginWithGoogle': 'Googleで続ける',
    'auth.or': 'または',
    'auth.pwdLen': '8文字以上',
    'auth.pwdUpper': '大文字を含む',
    'auth.pwdLower': '小文字を含む',
    'auth.pwdDigit': '数字を含む',
    'guide.views': '閲覧',
    'guide.tip': 'ヒント',
    'guide.location': '場所',
    'guide.locationPrivate': '場所非公開',
    'guide.noLocation': '場所情報なし',
    'guide.noExif': '撮影情報なし',
    'guide.edit': '編集',
    'guide.delete': '削除',
    'guide.editPrompt': '新しいヒントを入力:',
    'guide.deleteConfirm': 'このガイドを削除しますか？',
    'guide.likeLoginRequired': 'いいねするにはログインが必要です。',
    'guide.empty': '近くにガイドがありません。',
    'upload.title': 'ガイドをアップロード',
    'upload.submit': 'アップロード',
    'upload.tipLabel': '撮影ヒント',
    'upload.tipPlaceholder': 'このスポットの撮影ヒントを入力',
    'upload.locationToggle': '場所を公開',
    'upload.locationOn': '公開',
    'upload.locationOff': '非公開',
    'upload.fileHint': '写真または動画を選択',
    'profile.email': 'メールアドレス',
    'profile.noEmail': 'メール情報なし',
    'profile.deleteAccount': 'アカウント削除',
    'profile.deleteConfirm': 'アカウントを削除しますか？この操作は取り消せません。',
    'explore.radiusLabel': '半径',
    'explore.myLocation': '現在地',
    'explore.searchPlaceholder': '場所を検索',
    'explore.locateFailed': '現在地を取得できませんでした',
    'explore.zoomInHint': 'ズームインするとこのエリアのガイドが表示されます',
    'common.loading': '読み込み中...',
    'common.save': '保存',
    'common.cancel': 'キャンセル',
    'common.error': 'エラーが発生しました。',
    'settings.uiLanguage': '言語',
    'settings.appearance': '表示モード',
    'settings.themeLight': 'ライト',
    'settings.themeDark': 'ダーク',
  },
  zh: {
    'nav.explore': '探索',
    'nav.myGuides': '攻略',
    'nav.upload': '上传',
    'nav.profile': '我的',
    'nav.login': '登录',
    'nav.logout': '退出',
    'landing.title': 'SnapGuide',
    'landing.tagline': '将旅行瞬间变成攻略，\n与世界分享',
    'landing.feat1.title': '位置导航',
    'landing.feat1.desc': '发现附近的拍摄地点',
    'landing.feat2.title': '分享拍摄技巧',
    'landing.feat2.desc': '分享最佳角度和设置',
    'landing.feat3.title': '相机EXIF',
    'landing.feat3.desc': '同时记录真实的拍摄数据',
    'auth.login': '登录',
    'auth.signup': '注册',
    'auth.email': '邮箱',
    'auth.password': '密码',
    'auth.loginWithGoogle': '使用Google继续',
    'auth.or': '或',
    'auth.pwdLen': '至少8个字符',
    'auth.pwdUpper': '包含大写字母',
    'auth.pwdLower': '包含小写字母',
    'auth.pwdDigit': '包含数字',
    'guide.views': '浏览',
    'guide.tip': '提示',
    'guide.location': '位置',
    'guide.locationPrivate': '位置不公开',
    'guide.noLocation': '无位置信息',
    'guide.noExif': '无拍摄信息',
    'guide.edit': '编辑',
    'guide.delete': '删除',
    'guide.editPrompt': '请输入新提示:',
    'guide.deleteConfirm': '确定要删除吗？',
    'guide.likeLoginRequired': '请登录后点赞。',
    'guide.empty': '附近没有攻略。',
    'upload.title': '上传攻略',
    'upload.submit': '上传',
    'upload.tipLabel': '拍摄提示',
    'upload.tipPlaceholder': '请输入该地点的拍摄提示',
    'upload.locationToggle': '公开位置',
    'upload.locationOn': '公开',
    'upload.locationOff': '不公开',
    'upload.fileHint': '选择照片或视频',
    'profile.email': '邮箱',
    'profile.noEmail': '无邮箱信息',
    'profile.deleteAccount': '删除账号',
    'profile.deleteConfirm': '确定要删除账号吗？此操作无法撤销。',
    'explore.radiusLabel': '半径',
    'explore.myLocation': '我的位置',
    'explore.searchPlaceholder': '搜索地点',
    'explore.locateFailed': '无法获取当前位置',
    'explore.zoomInHint': '放大地图可查看该地区的导览',
    'common.loading': '加载中...',
    'common.save': '保存',
    'common.cancel': '取消',
    'common.error': '发生错误。',
    'settings.uiLanguage': '语言',
    'settings.appearance': '显示模式',
    'settings.themeLight': '浅色',
    'settings.themeDark': '深色',
  },
}

export function normalizeUiLanguage(value: string | null | undefined): UiLanguage {
  return SUPPORTED_UI_LANGUAGES.includes(value as UiLanguage)
    ? (value as UiLanguage)
    : 'ko'
}
