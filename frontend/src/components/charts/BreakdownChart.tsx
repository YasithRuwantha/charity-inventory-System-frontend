import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Cell } from "recharts";
import type { ReportBreakdownEntry } from "@/api/reports";
import { CHART_COLORS } from "@/components/charts/TrendChart";

interface BreakdownChartProps {
  data: ReportBreakdownEntry[];
  valueKey?: "count" | "quantity";
  height?: number;
  layout?: "horizontal" | "vertical";
}

export function BreakdownChart({ data, valueKey = "count", height = 220, layout = "vertical" }: BreakdownChartProps) {
  const chartData = data.slice(0, 8);

  if (layout === "vertical") {
    return (
      <ResponsiveContainer width="100%" height={Math.max(height, chartData.length * 34)}>
        <BarChart data={chartData} layout="vertical" margin={{ top: 4, right: 16, left: 8, bottom: 4 }} barCategoryGap={10}>
          <CartesianGrid strokeDasharray="3 3" horizontal={false} stroke="rgba(15,23,42,0.08)" />
          <XAxis type="number" tick={{ fontSize: 12, fill: "#64748b" }} axisLine={false} tickLine={false} allowDecimals={false} />
          <YAxis
            type="category"
            dataKey="label"
            tick={{ fontSize: 12, fill: "#334155" }}
            axisLine={false}
            tickLine={false}
            width={110}
          />
          <Tooltip
            cursor={{ fill: "rgba(15,118,110,0.06)" }}
            contentStyle={{ borderRadius: 10, border: "1px solid rgba(15,23,42,0.08)", fontSize: 13 }}
          />
          <Bar dataKey={valueKey} radius={[0, 6, 6, 0]} maxBarSize={18}>
            {chartData.map((_, i) => (
              <Cell key={i} fill={CHART_COLORS[i % CHART_COLORS.length]} />
            ))}
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    );
  }

  return (
    <ResponsiveContainer width="100%" height={height}>
      <BarChart data={chartData} margin={{ top: 8, right: 8, left: -16, bottom: 0 }}>
        <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="rgba(15,23,42,0.08)" />
        <XAxis dataKey="label" tick={{ fontSize: 12, fill: "#64748b" }} axisLine={{ stroke: "rgba(15,23,42,0.08)" }} tickLine={false} />
        <YAxis tick={{ fontSize: 12, fill: "#64748b" }} axisLine={false} tickLine={false} width={36} allowDecimals={false} />
        <Tooltip cursor={{ fill: "rgba(15,118,110,0.06)" }} contentStyle={{ borderRadius: 10, border: "1px solid rgba(15,23,42,0.08)", fontSize: 13 }} />
        <Bar dataKey={valueKey} radius={[6, 6, 0, 0]} maxBarSize={40}>
          {chartData.map((_, i) => (
            <Cell key={i} fill={CHART_COLORS[i % CHART_COLORS.length]} />
          ))}
        </Bar>
      </BarChart>
    </ResponsiveContainer>
  );
}
