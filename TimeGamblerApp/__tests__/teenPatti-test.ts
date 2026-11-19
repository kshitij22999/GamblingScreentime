import {canGamble, clampStake, maxStakeMinutes, newGameId} from '../src/teenPatti';

describe('stake picker rules', () => {
  it('needs at least 5 minutes to gamble', () => {
    expect(canGamble(299)).toBe(false);
    expect(canGamble(300)).toBe(true);
  });

  it('caps the picker at whole 5-minute steps and 6 hours', () => {
    expect(maxStakeMinutes(17 * 60 + 30)).toBe(15);
    expect(maxStakeMinutes(10 * 3600)).toBe(360);
    expect(maxStakeMinutes(200)).toBe(0);
  });

  it('clamps stakes into the legal range', () => {
    expect(clampStake(1, 3600)).toBe(5);
    expect(clampStake(500, 3600)).toBe(60);
    expect(clampStake(12, 3600)).toBe(10);
    expect(clampStake(15, 120)).toBe(0);
  });

  it('makes unique game ids', () => {
    const ids = new Set(Array.from({length: 100}, newGameId));
    expect(ids.size).toBe(100);
  });
});
