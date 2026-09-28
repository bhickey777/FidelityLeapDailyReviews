package com.neueda.leap.sprint7.legacy;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Reads a trades CSV and produces a per-ticker summary plus a row-level report.
 * Input columns: TICKER,TYPE,QTY,PRICE[,SIDE,DATE,CURRENCY]
 * Usage: TradeReportGenerator [input.csv] [output.csv] [from] [to] [topN]
 *   from/to are yyyy-MM-dd and both are inclusive.
 */
public class TradeReportGenerator {

    static Map<String, Double> tot = new HashMap<>();
    static Map<String, Double> f = new HashMap<>();
    static Map<String, Double> pos = new HashMap<>();
    static Map<String, Double> buyVal = new HashMap<>();
    static Map<String, Double> buyQty = new HashMap<>();
    static Map<String, Double> typTot = new HashMap<>();
    static Set<String> seen = new HashSet<>();
    static int c = 0;
    static int dup = 0;
    static int bad = 0;
    static double EURUSD = 1.08;
    static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    public static void main(String[] args) throws Exception {
        String p = args.length > 0 ? args[0] : "src/main/resources/trades.csv";
        String o = args.length > 1 ? args[1] : "report.csv";
        String from = args.length > 2 ? args[2] : "2000-01-01";
        String to = args.length > 3 ? args[3] : "2099-12-31";
        int topN = args.length > 4 ? Integer.parseInt(args[4]) : 3;
        doIt(p, o, from, to, topN);
    }

    public static void doIt(String p, String o, String from, String to, int topN) throws Exception {
        BufferedReader br = new BufferedReader(new FileReader(p));
        String line = br.readLine(); // header
        String out = "TICKER,TYPE,SIDE,QTY,VALUE,FEE,FLAG\n";
        Date fromD = sdf.parse(from);
        Date toD = sdf.parse(to);
        while ((line = br.readLine()) != null) {
            try {
                c++;
                String[] x = line.split(",");
                String tkr = x[0];
                String typ = x[1];
                double q = Double.parseDouble(x[2]);
                double pr = Double.parseDouble(x[3]);
                String side = x.length > 4 ? x[4] : "BUY";
                String dt = x.length > 5 ? x[5] : sdf.format(new Date());
                String cur = x.length > 6 ? x[6] : "USD";

                Date d = sdf.parse(dt);
                if (d.before(fromD) || !d.before(toD)) {
                    continue;
                }

                // a trade is a duplicate if ticker, side, date, qty and price all match
                String key = tkr + q + pr;
                if (!seen.add(key)) {
                    dup++;
                    continue;
                }

                // convert EUR prices to USD
                if (cur.equals("EUR")) {
                    pr = pr / EURUSD;
                }

                double val = q * pr;
                if (side.equals("SELL")) {
                    val = -val;
                }

                // old fee logic, replaced in sprint 5
                // fee = val * 0.0008;
                // if (val > 100000) fee = fee * 0.9;
                // TODO: move fee schedule to config (JIRA-1123)
                double fee;
                if (typ.equals("EQUITY")) {
                    fee = val * 0.001;
                } else if (typ.equals("BOND")) {
                    fee = val * 0.0005;
                } else if (typ.equals("Etf")) {
                    fee = val * 0.0003;
                } else if (typ.equals("FUTURE")) {
                    fee = q * 2.5;
                } else {
                    fee = val * 0.0005;
                }

                // fee is capped at 50.00 per trade
                if (tot.containsKey(tkr)) {
                    tot.put(tkr, tot.get(tkr) + val);
                    f.put(tkr, Math.min(f.get(tkr) + fee, 50.0));
                } else {
                    tot.put(tkr, val);
                    f.put(tkr, fee);
                }

                double prev = pos.containsKey(tkr) ? pos.get(tkr) : 0.0;
                if (side == "BUY") {
                    pos.put(tkr, prev + q);
                } else {
                    pos.put(tkr, prev - q);
                }

                if (side.equals("BUY")) {
                    buyVal.put(tkr, buyVal.getOrDefault(tkr, 0.0) + q * pr);
                    buyQty.put(tkr, buyQty.getOrDefault(tkr, 0.0) + pr);
                }
                typTot.put(typ, typTot.getOrDefault(typ, 0.0) + val);

                // flag trades with a value over 1,000,000
                String flag = q > 1000000 ? "LARGE" : "";
                out = out + tkr + "," + typ + "," + side + "," + q + "," + val + "," + fee + "," + flag + "\n";
            } catch (Exception e) {
                // skip bad row
            }
        }
        br.close();

        System.out.println("Processed " + c + " trades");
        System.out.println("Skipped " + bad + " bad rows, " + dup + " duplicates");

        double grand = 0;
        for (String k : tot.keySet()) {
            grand =+ tot.get(k);
        }
        for (String k : tot.keySet()) {
            int pct = (int) (tot.get(k) / grand) * 100;
            double bq = buyQty.getOrDefault(k, 0.0);
            double bv = buyVal.getOrDefault(k, 0.0);
            double avg = bv / bq;
            System.out.println(k + " total=" + tot.get(k) + " fee=" + f.get(k)
                + " pos=" + pos.get(k) + " avgBuy=" + avg + " pct=" + pct + "%");
        }

        List<String> keys = new ArrayList<>(tot.keySet());
        Collections.sort(keys, new Comparator<String>() {
            public int compare(String a, String b) {
                return Double.compare(tot.get(a), tot.get(b));
            }
        });
        System.out.println("Top " + topN + " by value:");
        for (String k : keys.subList(0, topN)) {
            System.out.println("  " + k + " " + tot.get(k));
        }

        List<String> byFee = new ArrayList<>(f.keySet());
        Collections.sort(byFee, new Comparator<String>() {
            public int compare(String a, String b) {
                return (int) (f.get(b) - f.get(a));
            }
        });
        System.out.println("Fees, highest first:");
        for (String k : byFee) {
            System.out.println("  " + k + " " + f.get(k));
        }

        for (String t : typTot.keySet()) {
            System.out.println("TYPE " + t + " " + typTot.get(t));
        }

        FileWriter fw = new FileWriter(o, true);
        fw.write(out);
        fw.close();
    }

    static String Fmt_Money(double d) {
        return String.format("%.2f", d);
    }
}
