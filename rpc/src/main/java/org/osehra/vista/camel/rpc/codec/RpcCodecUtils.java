/*
 * Copyright 2012-2013 The Open Source Electronic Health Record Agent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.osehra.vista.camel.rpc.codec;

import java.io.UnsupportedEncodingException;
import java.util.List;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.osehra.vista.camel.api.EmptyParameter;
import org.osehra.vista.camel.api.GlobalParameter;
import org.osehra.vista.camel.api.LiteralParameter;
import org.osehra.vista.camel.api.Parameter;
import org.osehra.vista.camel.api.ReferenceParameter;
import org.osehra.vista.camel.rpc.RpcConstants;
import org.osehra.vista.camel.rpc.RpcRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class RpcCodecUtils {
    private static final Logger LOG = LoggerFactory.getLogger(RpcCodecUtils.class);

    public static ChannelBuffer encodeRequest(final RpcRequest request) {
        return encodeRequest(request, true);
    }

    public static ChannelBuffer encodeRequest(final RpcRequest request, boolean encodeEmptyParamList) {
        ChannelBuffer cb = ChannelBuffers.dynamicBuffer();
        
        try {
            cb.writeByte((byte) RpcConstants.NS_START);
            cb.writeBytes(request.getNamespace().getBytes("UTF-8"));
            cb.writeByte((byte) RpcConstants.NS_STOP);

            cb.writeBytes(request.getCode().getBytes("UTF-8"));
            
            encodeText(request.getVersion(), cb);
            encodeText(request.getName(), cb);
            
            List<Parameter> params = request.getParmeters();
            if (params.size() > 0 || encodeEmptyParamList) {
                cb.writeByte((byte) RpcConstants.PARAMS_START);
                if (params.size() > 0) {
                    for (Parameter p : params) {
                        encodeParameter(p, cb);
                    }
                } else {
                    encodeEmptyParameter(cb);
                }
            }
            cb.writeByte((byte) RpcConstants.FRAME_STOP);
        } catch (UnsupportedEncodingException e) {
            LOG.warn("Failed to encode request", e);
        }
        return cb;
    }

    public static void encodeText(final String text, ChannelBuffer out) throws UnsupportedEncodingException {
        if (text != null && text.length() > 0) {
            out.writeByte(text.length());
            out.writeBytes(text.getBytes("UTF-8"));
        }
    }

    // TODO: refactor these as the default value of PARAM_PACK_LEN cannot be easily overriden, need a profile or something
    public static void encodeField(final String text, ChannelBuffer out) throws UnsupportedEncodingException {
        encodeField(text, RpcConstants.PARAM_PACK_LEN, out);
    }

    public static void encodeField(final String text, int lenlen, ChannelBuffer out) throws UnsupportedEncodingException {
        String f = "%0" + lenlen + "d"; // left pad with 0's up to length of lenlen
        out.writeBytes(String.format(f, text.length()).getBytes("UTF-8"));
        out.writeBytes(text.getBytes("UTF-8"));
    }

    public static void encodeParameter(final Parameter param, ChannelBuffer out) throws UnsupportedEncodingException {
        // TODO: there is a more elegant way of doing this
        if (param instanceof LiteralParameter) {
            encodeLiteralParameter((LiteralParameter)param, out);
        } else if (param instanceof ReferenceParameter) {
            encodeRefParameter((ReferenceParameter)param, out);
        } else if (param instanceof GlobalParameter) {
            encodeGlobalParameter((GlobalParameter)param, out);
        } else if (param instanceof EmptyParameter) {
            encodeEmptyParameter((EmptyParameter)param, out);
        }
    }

    public static void encodeLiteralParameter(final LiteralParameter param, ChannelBuffer out) throws UnsupportedEncodingException {
        out.writeByte(RpcConstants.PARAM_TYPE_LITERAL);
        encodeField(param.getValue(), out);
        out.writeByte(RpcConstants.PARAM_STOP);
    }

    public static void encodeRefParameter(final ReferenceParameter param, ChannelBuffer out) throws UnsupportedEncodingException {
        out.writeByte(RpcConstants.PARAM_TYPE_REF);
        encodeField(param.getValue(), out);
        out.writeByte(RpcConstants.PARAM_STOP);
    }

    public static void encodeGlobalParameter(final GlobalParameter param, ChannelBuffer out) throws UnsupportedEncodingException {
        out.writeByte(RpcConstants.PARAM_TYPE_GLOBAL);
        encodeField(param.getKey(), out);
        encodeField(param.getValue(), out);
        out.writeByte(RpcConstants.PARAM_STOP);
    }

    public static void encodeEmptyParameter(final EmptyParameter param, ChannelBuffer out) throws UnsupportedEncodingException {
        encodeEmptyParameter(out);
    }

    public static void encodeEmptyParameter(ChannelBuffer out) throws UnsupportedEncodingException {
        out.writeByte(RpcConstants.PARAM_TYPE_EMPTY);
        out.writeByte(RpcConstants.PARAM_STOP);
    }

    private RpcCodecUtils() {
        // Utility class
    }

}