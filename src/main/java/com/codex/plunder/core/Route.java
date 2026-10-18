package com.codex.plunder.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The shortest walk through a room's urns. It starts from where the player stands, visits the
 * urns worth searching once each and ends at the last of them. When the plan wants fewer than
 * the room holds, it takes whichever of them make the shortest walk. A room has at most fifteen
 * urns, so the walk is found exactly rather than guessed.
 */
public final class Route
{
	private Route()
	{
	}

	/**
	 * @param fromX the player's tile
	 * @param xs the urns' tiles, x
	 * @param ys the urns' tiles, y
	 * @param count how many urns to visit
	 * @return indexes into the urns, in walking order
	 */
	public static List<Integer> order(int fromX, int fromY, int[] xs, int[] ys, int count)
	{
		int n = Math.min(xs.length, ys.length);
		count = Math.max(0, Math.min(count, n));
		if (count == 0)
		{
			return Collections.emptyList();
		}
		if (n <= EXACT)
		{
			return exact(fromX, fromY, xs, ys, n, count);
		}
		// Nearest first gives a sound walk; then untangle it by reversing any stretch that shortens it.
		List<Integer> path = new ArrayList<>();
		boolean[] used = new boolean[n];
		int cx = fromX;
		int cy = fromY;
		for (int k = 0; k < count; k++)
		{
			int best = -1;
			int bestD = Integer.MAX_VALUE;
			for (int i = 0; i < n; i++)
			{
				int d = steps(cx, cy, xs[i], ys[i]);
				if (!used[i] && d < bestD)
				{
					best = i;
					bestD = d;
				}
			}
			used[best] = true;
			path.add(best);
			cx = xs[best];
			cy = ys[best];
		}
		boolean better = true;
		while (better)
		{
			better = false;
			for (int i = 0; i < path.size() - 1; i++)
			{
				for (int j = i + 1; j < path.size(); j++)
				{
					if (gain(path, i, j, fromX, fromY, xs, ys) > 0)
					{
						Collections.reverse(path.subList(i, j + 1));
						better = true;
					}
				}
			}
		}
		return path;
	}

	/** Up to this many urns the walk is found exactly: 2^15 subsets is quick. */
	static final int EXACT = 15;

	/** Held-Karp over subsets: the shortest open walk from the start through exactly count urns. */
	private static List<Integer> exact(int fromX, int fromY, int[] xs, int[] ys, int n, int count)
	{
		int full = 1 << n;
		int[] best = new int[full * n];
		byte[] prev = new byte[full * n];
		java.util.Arrays.fill(best, Integer.MAX_VALUE);
		for (int i = 0; i < n; i++)
		{
			best[(1 << i) * n + i] = steps(fromX, fromY, xs[i], ys[i]);
			prev[(1 << i) * n + i] = -1;
		}
		int bestLen = Integer.MAX_VALUE;
		int bestMask = 0;
		int bestEnd = 0;
		for (int mask = 1; mask < full; mask++)
		{
			int size = Integer.bitCount(mask);
			if (size > count)
			{
				continue;
			}
			for (int end = 0; end < n; end++)
			{
				int here = best[mask * n + end];
				if (here == Integer.MAX_VALUE)
				{
					continue;
				}
				if (size == count)
				{
					if (here < bestLen)
					{
						bestLen = here;
						bestMask = mask;
						bestEnd = end;
					}
					continue;
				}
				for (int next = 0; next < n; next++)
				{
					if ((mask & (1 << next)) != 0)
					{
						continue;
					}
					int to = (mask | (1 << next)) * n + next;
					int len = here + steps(xs[end], ys[end], xs[next], ys[next]);
					if (len < best[to])
					{
						best[to] = len;
						prev[to] = (byte) end;
					}
				}
			}
		}
		List<Integer> path = new ArrayList<>();
		int mask = bestMask;
		int end = bestEnd;
		while (end >= 0)
		{
			path.add(end);
			int p = prev[mask * n + end];
			mask &= ~(1 << end);
			end = p;
		}
		Collections.reverse(path);
		return path;
	}

	/** Tiles walked along a path, from the start. */
	public static int length(int fromX, int fromY, int[] xs, int[] ys, List<Integer> path)
	{
		int total = 0;
		int cx = fromX;
		int cy = fromY;
		for (int i : path)
		{
			total += steps(cx, cy, xs[i], ys[i]);
			cx = xs[i];
			cy = ys[i];
		}
		return total;
	}

	/** How much shorter the walk gets by reversing path[i..j]. The path is open at its end. */
	private static int gain(List<Integer> path, int i, int j, int fromX, int fromY, int[] xs, int[] ys)
	{
		int ax = i == 0 ? fromX : xs[path.get(i - 1)];
		int ay = i == 0 ? fromY : ys[path.get(i - 1)];
		int b = path.get(i);
		int c = path.get(j);
		int before = steps(ax, ay, xs[b], ys[b]);
		int after = steps(ax, ay, xs[c], ys[c]);
		if (j + 1 < path.size())
		{
			int d = path.get(j + 1);
			before += steps(xs[c], ys[c], xs[d], ys[d]);
			after += steps(xs[b], ys[b], xs[d], ys[d]);
		}
		return before - after;
	}

	/** Steps between two tiles; the game walks diagonally as cheaply as straight. */
	static int steps(int x1, int y1, int x2, int y2)
	{
		return Math.max(Math.abs(x1 - x2), Math.abs(y1 - y2));
	}
}
