const FACET_LINES = 'M16 3 L16 29 M16 16 L29 16 M16 16 L3 16 M9.5 9.5 L22.5 9.5'

function Mark({ height }: { height: number }) {
  return (
    <svg viewBox="0 0 32 32" height={height} width={height} role="img" aria-label="QuantEdge">
      <polygon points="16,3 3,16 16,29" fill="#0F172A" />
      <polygon points="16,3 29,16 16,29" fill="#2563EB" />
      <path
        d={FACET_LINES}
        stroke="#FFFFFF"
        strokeWidth="0.6"
        strokeLinecap="round"
        fill="none"
        opacity="0.85"
      />
    </svg>
  )
}

function MonoMark({ height }: { height: number }) {
  return (
    <svg viewBox="0 0 32 32" height={height} width={height} role="img" aria-label="QuantEdge">
      <polygon points="16,3 3,16 16,29" fill="currentColor" fillOpacity="0.55" />
      <polygon points="16,3 29,16 16,29" fill="currentColor" fillOpacity="0.9" />
      <path
        d={`M16 3 L3 16 L16 29 L29 16 Z ${FACET_LINES}`}
        stroke="currentColor"
        strokeWidth="0.6"
        strokeLinecap="round"
        strokeLinejoin="round"
        fill="none"
      />
    </svg>
  )
}

function FullLockup({ height }: { height: number }) {
  return (
    <svg
      viewBox="0 0 172 32"
      height={height}
      width={(172 / 32) * height}
      role="img"
      aria-label="QuantEdge"
    >
      <polygon points="16,3 3,16 16,29" fill="#0F172A" />
      <polygon points="16,3 29,16 16,29" fill="#2563EB" />
      <path
        d={FACET_LINES}
        stroke="#FFFFFF"
        strokeWidth="0.6"
        strokeLinecap="round"
        fill="none"
        opacity="0.85"
      />
      <text
        x="42"
        y="22"
        fontFamily="var(--font-sans), Inter, ui-sans-serif, system-ui, sans-serif"
        fontSize="20"
        fontWeight="600"
        letterSpacing="-0.2"
      >
        <tspan fill="#0F172A">Quant</tspan>
        <tspan fill="#2563EB">Edge</tspan>
      </text>
    </svg>
  )
}

type LogoProps = {
  variant?: 'full' | 'mark' | 'mono'
  size?: number
  className?: string
}

export default function Logo({ variant = 'full', size = 32, className }: LogoProps) {
  return (
    <span className={className}>
      {variant === 'full' && <FullLockup height={size} />}
      {variant === 'mark' && <Mark height={size} />}
      {variant === 'mono' && <MonoMark height={size} />}
    </span>
  )
}
