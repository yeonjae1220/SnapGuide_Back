export type Exif = {
  manufacturer?: string
  model?: string
  iso?: number
  aperture?: string
  shutterSpeed?: string
  focalLength?: number
}

export type Media = {
  url: string
  exif?: Exif
}

export type Author = {
  email?: string
}

export type Guide = {
  id: number
  tip: string
  latitude?: number
  longitude?: number
  locationName?: string
  locationPublic?: boolean
  viewCount?: number
  likeCount?: number
  userHasLiked?: boolean
  media?: Media[]
  author?: Author
}
