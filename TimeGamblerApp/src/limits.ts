// Mirrors TimerRules.kt for UI controls only; Kotlin re-validates every change.
export const LIMIT_STEP_MINUTES = 5;
export const MIN_LIMIT_MINUTES = 5;
export const MAX_LIMIT_MINUTES = 120;

export function clampLimit(minutes: number): number {
  const stepped = Math.round(minutes / LIMIT_STEP_MINUTES) * LIMIT_STEP_MINUTES;
  return Math.min(MAX_LIMIT_MINUTES, Math.max(MIN_LIMIT_MINUTES, stepped));
}
