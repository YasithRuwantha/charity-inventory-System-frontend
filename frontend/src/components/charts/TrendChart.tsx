import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from "recharts";
import type { TrendPoint } from "@/api/reports";

interface TrendSeries {
  key: string;
  label: string;
  color: string;
  data: TrendPoint[];
}

interface TrendChartProps {
  series: TrendSeries[];
  valueKey?: "count" | "quantity";
  height?: number;
}

function monthLabel(period: string) {
  const [year, month] = period.split("-");
  const date = new Date(Number(year), Number(month) - 1, 1);
  return date.toLocaleDateString("en-US", { month: "short" });
}

export function TrendChart({ series, valueKey = "quantity", height = 220 }: TrendChartProps) {
  const periods = series[0]?.data.map((d) => d.period) ?? [];
  const chartData = periods.map((period, i) => {
    const row: Record<string, string | number> = { period, label: monthLabel(period) };
    series.forEach((s) => {
      row[s.key] = s.data[i]?.[valueKey] ?? 0;
    });
    return row;
  });

  return (
    <ResponsiveContainer width="100%" height={height}>
      <AreaChart data={chartData} margin={{ top: 8, right: 8, left: -16, bottom: 0 }}>
        <defs>
          {series.map((s) => (
            <linearGradient key={s.key} id={`trend-${s.key}`} x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor={s.color} stopOpacity={0.28} />
              <stop offset="100%" stopColor={s.color} stopOpacity={0} />
            </linearGradient>
          ))}
        </defs>
        <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e2e8f0" />
        <XAxis dataKey="label" tick={{ fontSize: 12, fill: "#64748b" }} axisLine={{ stroke: "#e2e8f0" }} tickLine={false} />
        <YAxis tick={{ fontSize: 12, fill: "#64748b" }} axisLine={false} tickLine={false} width={36} allowDecimals={false} />
        <Tooltip
          contentStyle={{
            borderRadius: 10,
            border: "1px solid #e2e8f0",
            boxShadow: "0 8px 24px -6px rgb(15 23 42 / 0.15)",
            fontSize: 13,
          }}
          labelStyle={{ color: "#0f172a", fontWeight: 600, marginBottom: 2 }}
          formatter={(value, name) => [value, series.find((s) => s.key === name)?.label ?? name]}
        />
        {series.map((s) => (
          <Area
            key={s.key}
            type="monotone"
            dataKey={s.key}
            name={s.key}
            stroke={s.color}
            strokeWidth={2.5}
            fill={`url(#trend-${s.key})`}
            isAnimationActive={false}
          />
        ))}
      </AreaChart>
    </ResponsiveContainer>
  );
}
