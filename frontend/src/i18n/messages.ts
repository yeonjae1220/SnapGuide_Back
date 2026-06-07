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
  | 'landing.mobileValue'
  | 'auth.login'
  | 'auth.signup'
  | 'auth.email'
  | 'auth.password'
  | 'auth.loginWithGoogle'
  | 'auth.or'
  | 'auth.continueTitle'
  | 'auth.emailDivider'
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
  | 'guide.moreActions'
  | 'guide.likeLoginRequired'
  | 'guide.empty'
  | 'guide.emptyAction'
  | 'guide.saveEdit'
  | 'guide.regionGuides'
  | 'guide.currentArea'
  | 'upload.title'
  | 'upload.submit'
  | 'upload.tipLabel'
  | 'upload.tipPlaceholder'
  | 'upload.locationToggle'
  | 'upload.locationOn'
  | 'upload.locationOff'
  | 'upload.locationNone'
  | 'upload.locationHelp'
  | 'upload.fileHint'
  | 'upload.addMore'
  | 'upload.removeFile'
  | 'upload.progress'
  | 'upload.processing'
  | 'profile.email'
  | 'profile.noEmail'
  | 'profile.deleteAccount'
  | 'profile.deleteConfirm'
  | 'explore.radiusLabel'
  | 'explore.myLocation'
  | 'explore.searchPlaceholder'
  | 'explore.locateFailed'
  | 'explore.zoomInHint'
  | 'explore.sheetTitle'
  | 'explore.sheetSubtitle'
  | 'explore.expandSheet'
  | 'explore.collapseSheet'
  | 'explore.searchAction'
  | 'explore.mapLoading'
  | 'explore.mapError'
  | 'explore.retryMap'
  | 'explore.clearSearch'
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
    'landing.mobileValue': '사진이 있는 장소별 여행 팁을 빠르게 발견하세요',
    'auth.login': '로그인',
    'auth.signup': '회원가입',
    'auth.email': '이메일',
    'auth.password': '비밀번호',
    'auth.loginWithGoogle': 'Google로 계속하기',
    'auth.or': '또는',
    'auth.continueTitle': '가이드를 발견하고 공유하기',
    'auth.emailDivider': '이메일로 계속하기',
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
    'guide.moreActions': '더보기',
    'guide.likeLoginRequired': '좋아요를 누르려면 로그인이 필요합니다.',
    'guide.empty': '근처에 가이드가 없습니다.',
    'guide.emptyAction': '첫 가이드를 업로드해보세요',
    'guide.saveEdit': '수정 저장',
    'guide.regionGuides': '지역별 가이드',
    'guide.currentArea': '현재 지도 범위',
    'upload.title': '가이드 업로드',
    'upload.submit': '업로드',
    'upload.tipLabel': '촬영 팁',
    'upload.tipPlaceholder': '이 장소의 촬영 팁을 입력하세요',
    'upload.locationToggle': '위치 공개',
    'upload.locationOn': '공개',
    'upload.locationOff': '비공개',
    'upload.locationNone': '위치 저장 안 함',
    'upload.locationHelp': '업로드할 때 선택한 방식으로 현재 위치를 함께 저장합니다.',
    'upload.fileHint': '사진 또는 영상을 선택하세요',
    'upload.addMore': '더 추가',
    'upload.removeFile': '파일 삭제',
    'upload.progress': '업로드 중',
    'upload.processing': '사진과 위치 정보를 처리 중입니다',
    'profile.email': '이메일',
    'profile.noEmail': '이메일 정보 없음',
    'profile.deleteAccount': '계정 삭제',
    'profile.deleteConfirm': '정말 계정을 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.',
    'explore.radiusLabel': '반경',
    'explore.myLocation': '내 위치',
    'explore.searchPlaceholder': '장소 검색',
    'explore.locateFailed': '현재 위치를 찾을 수 없습니다',
    'explore.zoomInHint': '지도를 확대하면 이 지역의 가이드를 볼 수 있습니다',
    'explore.sheetTitle': '지도 속 가이드',
    'explore.sheetSubtitle': '핀을 누르거나 지도를 움직여 주변 팁을 확인하세요',
    'explore.expandSheet': '목록 크게 보기',
    'explore.collapseSheet': '목록 작게 보기',
    'explore.searchAction': '장소를 검색해보세요',
    'explore.mapLoading': '지도를 불러오는 중입니다',
    'explore.mapError': '지도를 불러오지 못했습니다',
    'explore.retryMap': '다시 시도',
    'explore.clearSearch': '검색어 지우기',
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
    'landing.mobileValue': 'Discover place-based travel tips with real photos',
    'auth.login': 'Log in',
    'auth.signup': 'Sign up',
    'auth.email': 'Email',
    'auth.password': 'Password',
    'auth.loginWithGoogle': 'Continue with Google',
    'auth.or': 'or',
    'auth.continueTitle': 'Discover and share guides',
    'auth.emailDivider': 'Continue with email',
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
    'guide.moreActions': 'More actions',
    'guide.likeLoginRequired': 'Log in to like guides.',
    'guide.empty': 'No guides nearby.',
    'guide.emptyAction': 'Upload the first guide',
    'guide.saveEdit': 'Save changes',
    'guide.regionGuides': 'Regional guides',
    'guide.currentArea': 'Current map area',
    'upload.title': 'Upload Guide',
    'upload.submit': 'Upload',
    'upload.tipLabel': 'Shooting tip',
    'upload.tipPlaceholder': 'Enter a tip for this spot',
    'upload.locationToggle': 'Share location',
    'upload.locationOn': 'Public',
    'upload.locationOff': 'Private',
    'upload.locationNone': 'Do not save location',
    'upload.locationHelp': 'Your current location is saved with the selected visibility when you upload.',
    'upload.fileHint': 'Select a photo or video',
    'upload.addMore': 'Add more',
    'upload.removeFile': 'Remove file',
    'upload.progress': 'Uploading',
    'upload.processing': 'Processing photo and location data',
    'profile.email': 'Email',
    'profile.noEmail': 'No email on record',
    'profile.deleteAccount': 'Delete account',
    'profile.deleteConfirm': 'Delete your account? This cannot be undone.',
    'explore.radiusLabel': 'Radius',
    'explore.myLocation': 'My location',
    'explore.searchPlaceholder': 'Search places',
    'explore.locateFailed': 'Could not find your location',
    'explore.zoomInHint': 'Zoom in to see guides in this area',
    'explore.sheetTitle': 'Guides on the map',
    'explore.sheetSubtitle': 'Tap pins or move the map to browse nearby tips',
    'explore.expandSheet': 'Expand list',
    'explore.collapseSheet': 'Collapse list',
    'explore.searchAction': 'Try searching a place',
    'explore.mapLoading': 'Loading map',
    'explore.mapError': 'Could not load the map',
    'explore.retryMap': 'Try again',
    'explore.clearSearch': 'Clear search',
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
    'landing.mobileValue': '写真付きの場所別旅行ヒントをすばやく発見',
    'auth.login': 'ログイン',
    'auth.signup': '新規登録',
    'auth.email': 'メールアドレス',
    'auth.password': 'パスワード',
    'auth.loginWithGoogle': 'Googleで続ける',
    'auth.or': 'または',
    'auth.continueTitle': 'ガイドを見つけて共有',
    'auth.emailDivider': 'メールで続ける',
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
    'guide.moreActions': 'その他',
    'guide.likeLoginRequired': 'いいねするにはログインが必要です。',
    'guide.empty': '近くにガイドがありません。',
    'guide.emptyAction': '最初のガイドを投稿',
    'guide.saveEdit': '変更を保存',
    'guide.regionGuides': '地域別ガイド',
    'guide.currentArea': '現在の地図範囲',
    'upload.title': 'ガイドをアップロード',
    'upload.submit': 'アップロード',
    'upload.tipLabel': '撮影ヒント',
    'upload.tipPlaceholder': 'このスポットの撮影ヒントを入力',
    'upload.locationToggle': '場所を公開',
    'upload.locationOn': '公開',
    'upload.locationOff': '非公開',
    'upload.locationNone': '場所を保存しない',
    'upload.locationHelp': 'アップロード時に選択した公開範囲で現在地を保存します。',
    'upload.fileHint': '写真または動画を選択',
    'upload.addMore': 'さらに追加',
    'upload.removeFile': 'ファイル削除',
    'upload.progress': 'アップロード中',
    'upload.processing': '写真と位置情報を処理中',
    'profile.email': 'メールアドレス',
    'profile.noEmail': 'メール情報なし',
    'profile.deleteAccount': 'アカウント削除',
    'profile.deleteConfirm': 'アカウントを削除しますか？この操作は取り消せません。',
    'explore.radiusLabel': '半径',
    'explore.myLocation': '現在地',
    'explore.searchPlaceholder': '場所を検索',
    'explore.locateFailed': '現在地を取得できませんでした',
    'explore.zoomInHint': 'ズームインするとこのエリアのガイドが表示されます',
    'explore.sheetTitle': '地図上のガイド',
    'explore.sheetSubtitle': 'ピンをタップするか地図を動かして周辺のヒントを確認',
    'explore.expandSheet': 'リストを拡大',
    'explore.collapseSheet': 'リストを縮小',
    'explore.searchAction': '場所を検索してみましょう',
    'explore.mapLoading': '地図を読み込み中',
    'explore.mapError': '地図を読み込めませんでした',
    'explore.retryMap': '再試行',
    'explore.clearSearch': '検索をクリア',
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
    'landing.mobileValue': '快速发现带照片的地点旅行提示',
    'auth.login': '登录',
    'auth.signup': '注册',
    'auth.email': '邮箱',
    'auth.password': '密码',
    'auth.loginWithGoogle': '使用Google继续',
    'auth.or': '或',
    'auth.continueTitle': '发现并分享攻略',
    'auth.emailDivider': '使用邮箱继续',
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
    'guide.moreActions': '更多',
    'guide.likeLoginRequired': '请登录后点赞。',
    'guide.empty': '附近没有攻略。',
    'guide.emptyAction': '上传第一个攻略',
    'guide.saveEdit': '保存修改',
    'guide.regionGuides': '地区攻略',
    'guide.currentArea': '当前地图范围',
    'upload.title': '上传攻略',
    'upload.submit': '上传',
    'upload.tipLabel': '拍摄提示',
    'upload.tipPlaceholder': '请输入该地点的拍摄提示',
    'upload.locationToggle': '公开位置',
    'upload.locationOn': '公开',
    'upload.locationOff': '不公开',
    'upload.locationNone': '不保存位置',
    'upload.locationHelp': '上传时会按所选可见性保存当前位置。',
    'upload.fileHint': '选择照片或视频',
    'upload.addMore': '继续添加',
    'upload.removeFile': '删除文件',
    'upload.progress': '上传中',
    'upload.processing': '正在处理照片和位置信息',
    'profile.email': '邮箱',
    'profile.noEmail': '无邮箱信息',
    'profile.deleteAccount': '删除账号',
    'profile.deleteConfirm': '确定要删除账号吗？此操作无法撤销。',
    'explore.radiusLabel': '半径',
    'explore.myLocation': '我的位置',
    'explore.searchPlaceholder': '搜索地点',
    'explore.locateFailed': '无法获取当前位置',
    'explore.zoomInHint': '放大地图可查看该地区的导览',
    'explore.sheetTitle': '地图上的攻略',
    'explore.sheetSubtitle': '点击标记或移动地图查看附近提示',
    'explore.expandSheet': '展开列表',
    'explore.collapseSheet': '收起列表',
    'explore.searchAction': '试着搜索地点',
    'explore.mapLoading': '正在加载地图',
    'explore.mapError': '无法加载地图',
    'explore.retryMap': '重试',
    'explore.clearSearch': '清除搜索',
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
