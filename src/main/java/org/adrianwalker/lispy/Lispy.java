package org.adrianwalker.lispy;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

public final class Lispy {

  private class Env extends HashMap<String, Object> {

    private final Env outer;

    public Env(final List<String> params, final List<Object> args, final Env outer) {
      IntStream.range(0, params.size()).forEach(i -> put(params.get(i), args.get(i)));
      this.outer = outer;
    }

    public Env find(final Object var) {
      return this.containsKey(var) ? this : outer.find(var);
    }
  }

  private Env addGlobal(final Env env) {
    env.put("+", (Function<List<Double>, Double>) (List<Double> args) -> args.get(0) + args.get(1));
    env.put("-", (Function<List<Double>, Double>) (List<Double> args) -> args.get(0) - args.get(1));
    env.put("*", (Function<List<Double>, Double>) (List<Double> args) -> args.get(0) * args.get(1));
    env.put("/", (Function<List<Double>, Double>) (List<Double> args) -> args.get(0) / args.get(1));
    env.put(">", (Function<List<Double>, Boolean>) (List<Double> args) -> args.get(0) > args.get(1));
    env.put("<", (Function<List<Double>, Boolean>) (List<Double> args) -> args.get(0) < args.get(1));
    env.put(">=", (Function<List<Double>, Boolean>) (List<Double> args) -> args.get(0) >= args.get(1));
    env.put("<=", (Function<List<Double>, Boolean>) (List<Double> args) -> args.get(0) <= args.get(1));
    env.put("=", (Function<List<Double>, Boolean>) (List<Double> args) -> Objects.equals(args.get(0), args.get(1)));
    env.put("equal?", (Function<List<Double>, Boolean>) (List<Double> args) -> Objects.equals(args.get(0), args.get(1)));
    env.put("eq?", (Function<List<Object>, Boolean>) (List<Object> args) -> args.get(0).getClass().isInstance(args.get(1)));
    env.put("length", (Function<List<Object>, Integer>) (List<Object> args) -> args.size());
    env.put("cons", (Function<List<Object>, List<Object>>) (List<Object> args) -> Stream.concat(Stream.of(args.get(0)), Stream.of(args.get(1))).collect(Collectors.toList()));
    env.put("car", (Function<List<Object>, Object>) (List<Object> args) -> ((List) args.get(0)).get(0));
    env.put("cdr", (Function<List<Object>, List<Object>>) (List<Object> args) -> ((List) args.get(0)).subList(1, ((List) args.get(0)).size()));
    env.put("append", (Function<List<Object>, List<Object>>) (List<Object> args) -> (List) Stream.concat(((List) args.get(0)).stream(), ((List) args.get(1)).stream()).collect(Collectors.toList()));
    env.put("list", (Function<List<Object>, List<Object>>) (List<Object> args) -> args);
    env.put("list?", (Function<List<Object>, Boolean>) (List<Object> args) -> args.get(0) instanceof List);
    env.put("null?", (Function<List<Object>, Boolean>) (List<Object> args) -> ((List) args.get(0)).isEmpty());
    env.put("symbol?", (Function<List<Object>, Boolean>) (List<Object> args) -> args.get(0) instanceof String);
    return env;
  }

  private final Env globalEnv = addGlobal(new Env(Collections.EMPTY_LIST, Collections.EMPTY_LIST, null));

  private Object eval(final Object x, final Env env) {
    if (x instanceof String) {
      return env.find(x).get(x);
    } else if (!(x instanceof List)) {
      return x;
    } else if (((List) x).get(0).equals("quote")) {
      return ((List) x).get(1);
    } else if (((List) x).get(0).equals("if")) {
      return eval(((Boolean) eval(((List) x).get(1), env)) ? ((List) x).get(2) : ((List) x).get(3), env);
    } else if (((List) x).get(0).equals("set!")) {
      env.find(((List) x).get(1)).put((String) ((List) x).get(1), eval(((List) x).get(2), env));
    } else if (((List) x).get(0).equals("define")) {
      env.put((String) ((List) x).get(1), eval(((List) x).get(2), env));
    } else if (((List) x).get(0).equals("lambda")) {
      return (Function<List<Object>, Object>) (List<Object> args) -> eval(((List) x).get(2), new Env((List) ((List) x).get(1), args, env));
    } else if (((List) x).get(0).equals("begin")) {
      Object val = null;
      for (Object exp : ((List) x).subList(1, ((List) x).size())) {
        val = eval(exp, env);
      }
      return val;
    } else {
      List exps = (List) ((List) x).stream().map(exp -> eval(exp, env)).collect(Collectors.toList());
      Function proc = (Function) exps.get(0);
      return proc.apply(exps.subList(1, exps.size()));
    }
    return null;
  }

  private Object read(final String s) {
    return readFrom(new ArrayDeque<>(Arrays.asList(tokenize(s))));
  }

  private String[] tokenize(final String s) {
    return s.replace("(", " ( ").replace(")", " ) ").trim().split("\\s+");
  }

  private Object readFrom(final Deque<String> tokens) {

    if (tokens.isEmpty()) {
      throw new RuntimeException("unexpected EOF while reading");
    }

    String token = tokens.pop();
    if ("(".equals(token)) {
      List l = new ArrayList();
      while (!tokens.peek().equals(")")) {
        l.add(readFrom(tokens));
      }
      tokens.pop();
      return l;
    } else if (")".equals(token)) {
      throw new RuntimeException("unexpected )");
    } else {
      return atom(token);
    }
  }

  private Object atom(final String token) {
    try {
      return Double.valueOf(token);
    } catch (final NumberFormatException nfe) {
      return token;
    }
  }

  private String toString(final Object exp) {
    return exp instanceof List ? "(" + ((List) exp).stream().map(x -> toString(x)).collect(Collectors.joining(" ")) + ")" : exp.toString();
  }

  private void repl() {
    while (true) {
      System.out.print("lispy> ");
      Object val = eval(read(new Scanner(System.in).nextLine()), globalEnv);
      if (null != val) {
        System.out.println(toString(val));
      }
    }
  }

  public static void main(final String[] args) {
    new Lispy().repl();
  }
}
