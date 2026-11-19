import {formatClock, formatDuration} from '../src/time';
import {clampLimit} from '../src/limits';

describe('formatClock', () => {
  it('always formats as HH:MM:SS', () => {
    expect(formatClock(0)).toBe('00:00:00');
    expect(formatClock(65)).toBe('00:01:05');
    expect(formatClock(59 * 60 + 59)).toBe('00:59:59');
    expect(formatClock(3600)).toBe('01:00:00');
    expect(formatClock(2 * 3600 + 5 * 60 + 9)).toBe('02:05:09');
  });

  it('never shows negative time', () => {
    expect(formatClock(-10)).toBe('00:00:00');
  });
});

describe('formatDuration', () => {
  it('rounds up to whole minutes', () => {
    expect(formatDuration(1)).toBe('1m');
    expect(formatDuration(60_000)).toBe('1m');
    expect(formatDuration(61_000)).toBe('2m');
  });

  it('shows hours and minutes', () => {
    expect(formatDuration(3_600_000)).toBe('1h');
    expect(formatDuration(23 * 3_600_000 + 5 * 60_000)).toBe('23h 5m');
  });
});

describe('clampLimit', () => {
  it('snaps to 5 minute steps within 5–120', () => {
    expect(clampLimit(0)).toBe(5);
    expect(clampLimit(7)).toBe(5);
    expect(clampLimit(8)).toBe(10);
    expect(clampLimit(125)).toBe(120);
    expect(clampLimit(60)).toBe(60);
  });
});
