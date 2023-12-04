package com.github.tvbox.osc.util.urlhttp.internal;

//dec-0.1.2.jar
import org.brotli.dec.BrotliInputStream;

import java.io.IOException;

import okio.BufferedSource;
import okio.Okio;
import okio.Source;

public final class BrotliSource {
  public static Source create(BufferedSource source) throws IOException {
    BrotliInputStream brotliInputStream = new BrotliInputStream(source.inputStream());
    return Okio.source(brotliInputStream);
  }
}
