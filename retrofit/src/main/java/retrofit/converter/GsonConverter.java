/*
 * Copyright (C) 2012 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package retrofit.converter;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import retrofit.mime.MimeUtil;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;

/**
 * A {@link Converter} which uses GSON for serialization and deserialization of entities.
 *
 * @author Jake Wharton (jw@squareup.com)
 */
public class GsonConverter implements Converter {

  public static final String APPLICATION_JSON_VALUE = "application/json";
  public static final String TEXT_HTML_VALUE = "text/html";

  private final Gson gson;
  private String mime;
  private String charset;

  /**
   * Create an instance using the supplied {@link Gson} object for conversion. Encoding to JSON and
   * decoding from JSON
   * (when no charset is specified by a header) will use UTF-8.
   * (when no mime is specified by a header) will use APPLICATION_JSON_VALUE.
   */
  public GsonConverter(Gson gson) {
    this(gson, "UTF-8");
  }

  /**
   * Create an instance using the supplied {@link Gson} object for conversion. Encoding to JSON and
   * decoding from JSON
   * (when no charset is specified by a header) will use UTF-8.
   */
  public GsonConverter(Gson gson, String mime) {
    this(gson, mime, "UTF-8");
  }

  /**
   * Create an instance using the supplied {@link Gson} object for conversion. Encoding to JSON and
   * decoding from JSON
   * (when no charset is specified by a header) will use the specified charset.
   */
  public GsonConverter(Gson gson, String charset) {
    this(gson, APPLICATION_JSON_VALUE, charset);
  }

  /**
   * Create an instance using the supplied {@link Gson} object for conversion. Encoding to JSON and
   * decoding from JSON.
   */
  public GsonConverter(Gson gson, String mime, String charset) {
    this.gson = gson;
    this.mime = mime;
    this.charset = charset;
  }

  @Override public Object fromBody(TypedInput body, Type type) throws ConversionException {
    String charset = this.charset;
    if (body.mimeType() != null) {
      charset = MimeUtil.parseCharset(body.mimeType(), charset);
    }
    InputStreamReader isr = null;
    try {
      isr = new InputStreamReader(body.in(), charset);
      return gson.fromJson(isr, type);
    } catch (IOException e) {
      throw new ConversionException(e);
    } catch (JsonParseException e) {
      throw new ConversionException(e);
    } finally {
      if (isr != null) {
        try {
          isr.close();
        } catch (IOException ignored) {
        }
      }
    }
  }

  @Override public TypedOutput toBody(Object object) {
    try {
      return new JsonTypedOutput(gson.toJson(object).getBytes(charset), mime, charset);
    } catch (UnsupportedEncodingException e) {
      throw new AssertionError(e);
    }
  }

  private static class JsonTypedOutput implements TypedOutput {
    private final byte[] jsonBytes;
    private final String mimeType;

    JsonTypedOutput(byte[] jsonBytes, String mime, String encode) {
      this.jsonBytes = jsonBytes;
      this.mimeType = mime + "; charset=" + encode;
    }

    @Override public String fileName() {
      return null;
    }

    @Override public String mimeType() {
      return mimeType;
    }

    @Override public long length() {
      return jsonBytes.length;
    }

    @Override public void writeTo(OutputStream out) throws IOException {
      out.write(jsonBytes);
    }
  }
}
