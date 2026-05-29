import type { Config } from 'tailwindcss'

const config: Config = {
  content: ['./src/**/*.{js,ts,jsx,tsx,mdx}'],
  theme: {
    extend: {
      fontFamily: {
        sans: ['DM Sans', 'var(--font-sans)', 'sans-serif'],
        display: ['Playfair Display', 'Georgia', 'serif'],
      },
      colors: {
        brand: {
          from: '#f09433',
          to: '#bc1888',
        },
      },
    },
  },
  plugins: [],
}

export default config
