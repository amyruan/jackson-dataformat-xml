package com.fasterxml.jackson.dataformat.xml;

import java.io.*;

import javax.xml.stream.*;

import org.codehaus.stax2.io.Stax2ByteArraySource;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.format.InputAccessor;
import com.fasterxml.jackson.core.format.MatchStrength;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import com.fasterxml.jackson.dataformat.xml.util.StaxUtil;


/**
* Factory used for constructing {@link FromXmlParser} and {@link ToXmlGenerator}
* instances.
*<p>
* Implements {@link JsonFactory} since interface for constructing XML backed
* parsers and generators is quite similar to dealing with JSON.
* 
* @author Tatu Saloranta (tatu.saloranta@iki.fi)
*/
public class XmlFactory extends JsonFactory
{
    /**
     * Name used to identify XML format
     * (and returned by {@link #getFormatName()}
     */
    public final static String FORMAT_NAME_XML = "XML";

    /**
     * Bitfield (set of flags) of all parser features that are enabled
     * by default.
     */
    final static int DEFAULT_XML_PARSER_FEATURE_FLAGS = FromXmlParser.Feature.collectDefaults();

    /**
     * Bitfield (set of flags) of all generator features that are enabled
     * by default.
     */
    final static int DEFAULT_XML_GENERATOR_FEATURE_FLAGS = ToXmlGenerator.Feature.collectDefaults();

    /*
    /**********************************************************
    /* Configuration
    /**********************************************************
     */

    protected int _xmlParserFeatures = DEFAULT_XML_PARSER_FEATURE_FLAGS;

    protected int _xmlGeneratorFeatures = DEFAULT_XML_GENERATOR_FEATURE_FLAGS;

    protected XMLInputFactory _xmlInputFactory;

    protected XMLOutputFactory _xmlOutputFactory;

    /*
    /**********************************************************
    /* Factory construction, configuration
    /**********************************************************
     */

    /**
     * Default constructor used to create factory instances.
     * Creation of a factory instance is a light-weight operation,
     * but it is still a good idea to reuse limited number of
     * factory instances (and quite often just a single instance):
     * factories are used as context for storing some reused
     * processing objects (such as symbol tables parsers use)
     * and this reuse only works within context of a single
     * factory instance.
     */
    public XmlFactory() { this(null); }

    public XmlFactory(ObjectCodec oc) {
        this(oc, null, null);
    }

    public XmlFactory(XMLInputFactory xmlIn, XMLOutputFactory xmlOut)
    {
        this(null, xmlIn, xmlOut);
    }
    
    public XmlFactory(ObjectCodec oc,
            XMLInputFactory xmlIn, XMLOutputFactory xmlOut)
    {
        super(oc);
        if (xmlIn == null) {
            /* 24-Jun-2010, tatu: Ugh. JDK authors seem to waffle on what the name of
             *   factory constructor method is...
             */
            //xmlIn = XMLInputFactory.newFactory();
            xmlIn = XMLInputFactory.newInstance();
        }
        if (xmlOut == null) {
            //xmlOut = XMLOutputFactory.newFactory();
            xmlOut = XMLOutputFactory.newInstance();
        }
        // Better ensure namespaces get built properly, so:
        xmlOut.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, Boolean.TRUE);
        // and for parser, force coalescing as well (much simpler to use)
        xmlIn.setProperty(XMLInputFactory.IS_COALESCING, true);
        _xmlInputFactory = xmlIn;
        _xmlOutputFactory = xmlOut;
    }

    @Override
    public Version version() {
        return ModuleVersion.instance.version();
    }
    
    /*
    /**********************************************************
    /* Configuration, parser settings
    /**********************************************************
     */

    /**
     * Method for enabling or disabling specified XML parser feature.
     */
    public final XmlFactory configure(FromXmlParser.Feature f, boolean state)
    {
        if (state) {
            enable(f);
        } else {
            disable(f);
        }
        return this;
    }

    /**
     * Method for enabling specified XML parser feature.
     */
    public XmlFactory enable(FromXmlParser.Feature f) {
        _xmlParserFeatures |= f.getMask();
        return this;
    }

    /**
     * Method for disabling specified XML parser feature.
     */
    public XmlFactory disable(FromXmlParser.Feature f) {
        _xmlParserFeatures &= ~f.getMask();
        return this;
    }

    /**
     * Checked whether specified XML parser feature is enabled.
     */
    public final boolean isEnabled(FromXmlParser.Feature f) {
        return (_xmlParserFeatures & f.getMask()) != 0;
    }

    /*
    /******************************************************
    /* Configuration, generator settings
    /******************************************************
     */

    /**
     * Method for enabling or disabling specified XML generator feature.
     */
    public final XmlFactory configure(ToXmlGenerator.Feature f, boolean state) {
        if (state) {
            enable(f);
        } else {
            disable(f);
        }
        return this;
    }


    /**
     * Method for enabling specified XML generator feature.
     */
    public XmlFactory enable(ToXmlGenerator.Feature f) {
        _xmlGeneratorFeatures |= f.getMask();
        return this;
    }

    /**
     * Method for disabling specified XML generator feature.
     */
    public XmlFactory disable(ToXmlGenerator.Feature f) {
        _xmlGeneratorFeatures &= ~f.getMask();
        return this;
    }

    /**
     * Check whether specified XML generator feature is enabled.
     */
    public final boolean isEnabled(ToXmlGenerator.Feature f) {
        return (_xmlGeneratorFeatures & f.getMask()) != 0;
    }

    /*
    /**********************************************************
    /* Additional configuration
    /**********************************************************
     */

    public void setXMLInputFactory(XMLInputFactory f) {
        _xmlInputFactory = f;
    }

    public void setXMLOutputFactory(XMLOutputFactory f) {
        _xmlOutputFactory = f;
    }

    /*
    /**********************************************************
    /* Format detection functionality (since 1.8)
    /**********************************************************
     */

    /**
     * Method that returns short textual id identifying format
     * this factory supports.
     *<p>
     * Note: sub-classes should override this method; default
     * implementation will return null for all sub-classes
     */
    public String getFormatName()
    {
        return FORMAT_NAME_XML;
    }

    public MatchStrength hasFormat(InputAccessor acc) throws IOException
    {
        return hasXMLFormat(acc);
    }

    /*
    /**********************************************************
    /* Upcoming parts of public API (for 2.1)
    /**********************************************************
     */

    // @Override
    public ToXmlGenerator createGenerator(OutputStream out, JsonEncoding enc) throws IOException
    {
        // false -> we won't manage the stream unless explicitly directed to
        return new ToXmlGenerator(_createContext(out, false),
                _generatorFeatures, _xmlGeneratorFeatures,
                _objectCodec, _createXmlWriter(out));
    }

    // @Override
    public ToXmlGenerator createGenerator(Writer out) throws IOException
    {
        return new ToXmlGenerator(_createContext(out, false),
                _generatorFeatures, _xmlGeneratorFeatures,
                _objectCodec, _createXmlWriter(out));
    }

    // @Override
    public ToXmlGenerator createGenerator(File f, JsonEncoding enc) throws IOException
    {
        OutputStream out = new FileOutputStream(f);
        // true -> yes, we have to manage the stream since we created it
        IOContext ctxt = _createContext(out, true);
        ctxt.setEncoding(enc);
        return new ToXmlGenerator(ctxt, _generatorFeatures, _xmlGeneratorFeatures,
                _objectCodec, _createXmlWriter(out));
    }
    
    /*
    /**********************************************************
    /* Overridden parts of public API for generator creation
    /**********************************************************
     */
    
    /**
     *<p>
     * note: co-variant return type
     */
    @Override
    public ToXmlGenerator createJsonGenerator(OutputStream out, JsonEncoding enc)
        throws IOException
    {
        // false -> we won't manage the stream unless explicitly directed to
        return new ToXmlGenerator(_createContext(out, false),
                _generatorFeatures, _xmlGeneratorFeatures,
                _objectCodec, _createXmlWriter(out));
    }

    @Override
    public ToXmlGenerator createJsonGenerator(Writer out)
        throws IOException
    {
        return new ToXmlGenerator(_createContext(out, false),
                _generatorFeatures, _xmlGeneratorFeatures,
                _objectCodec, _createXmlWriter(out));
    }

    @Override
    public ToXmlGenerator createJsonGenerator(File f, JsonEncoding enc)
        throws IOException
    {
        OutputStream out = new FileOutputStream(f);
        // true -> yes, we have to manage the stream since we created it
        IOContext ctxt = _createContext(out, true);
        ctxt.setEncoding(enc);
        return new ToXmlGenerator(ctxt, _generatorFeatures, _xmlGeneratorFeatures,
                _objectCodec, _createXmlWriter(out));
    }

    /*
    /**********************************************************
    /* Upcoming parts of public API (for 2.1)
    /**********************************************************
     */

//  @Override
    protected FromXmlParser _createParser(InputStream in, IOContext ctxt)
        throws IOException, JsonParseException
    {
        return _createJsonParser(in, ctxt);
    }

//    @Override
    protected FromXmlParser _createParser(Reader r, IOContext ctxt)
        throws IOException, JsonParseException
    {
        return _createJsonParser(r, ctxt);
    }

//  @Override
    protected FromXmlParser _createParser(byte[] data, int offset, int len, IOContext ctxt)
        throws IOException, JsonParseException
    {
        return _createJsonParser(data, offset, len, ctxt);
    }
    
    /*
    /**********************************************************
    /* Overridden internal factory methods for parser creation
    /**********************************************************
     */

    //protected IOContext _createContext(Object srcRef, boolean resourceManaged)

    /**
     * Overridable factory method that actually instantiates desired
     * parser.
     */
    @Override
    protected FromXmlParser _createJsonParser(InputStream in, IOContext ctxt)
        throws IOException, JsonParseException
    {
        XMLStreamReader sr;
        try {
            sr = _xmlInputFactory.createXMLStreamReader(in);
            sr = _initializeXmlReader(sr);
        } catch (XMLStreamException e) {
            return StaxUtil.throwXmlAsIOException(e);
        }
        return new FromXmlParser(ctxt, _generatorFeatures, _xmlGeneratorFeatures,
                _objectCodec, sr);
    }

    /**
     * Overridable factory method that actually instantiates desired
     * parser.
     */
    @Override
    protected FromXmlParser _createJsonParser(Reader r, IOContext ctxt)
        throws IOException, JsonParseException
    {
        XMLStreamReader sr;
        try {
            sr = _xmlInputFactory.createXMLStreamReader(r);
            sr = _initializeXmlReader(sr);
        } catch (XMLStreamException e) {
            return StaxUtil.throwXmlAsIOException(e);
        }
        return new FromXmlParser(ctxt, _generatorFeatures, _xmlGeneratorFeatures,
                _objectCodec, sr);
    }

    /**
     * Overridable factory method that actually instantiates desired
     * parser.
     */
    @Override
    protected FromXmlParser _createJsonParser(byte[] data, int offset, int len, IOContext ctxt)
        throws IOException, JsonParseException
    {
        XMLStreamReader sr;
        try {
            sr = _xmlInputFactory.createXMLStreamReader(new Stax2ByteArraySource(data, offset, len));
            sr = _initializeXmlReader(sr);
        } catch (XMLStreamException e) {
            return StaxUtil.throwXmlAsIOException(e);
        }
        return new FromXmlParser(ctxt, _generatorFeatures, _xmlGeneratorFeatures,
                _objectCodec, sr);
    }

    /*
    /**********************************************************************
    /* Internal factory methods
    /**********************************************************************
     */

    protected XMLStreamWriter _createXmlWriter(OutputStream out) throws IOException
    {
        try {
            return _initializeXmlWriter(_xmlOutputFactory.createXMLStreamWriter(out, "UTF-8"));
        } catch (XMLStreamException e) {
            return StaxUtil.throwXmlAsIOException(e);
        }
    }

    protected XMLStreamWriter _createXmlWriter(Writer w) throws IOException
    {
        try {
            return _initializeXmlWriter(_xmlOutputFactory.createXMLStreamWriter(w));
        } catch (XMLStreamException e) {
            return StaxUtil.throwXmlAsIOException(e);
        }
    }

    protected final XMLStreamWriter _initializeXmlWriter(XMLStreamWriter sw) throws IOException, XMLStreamException
    {
        // And just for Sun Stax parser (JDK default), seems that we better define default namespace
        // (Woodstox doesn't care) -- otherwise it'll add unnecessary odd declaration
        sw.setDefaultNamespace("");
        return sw;
    }

    protected final XMLStreamReader _initializeXmlReader(XMLStreamReader sr) throws IOException, XMLStreamException
    {
        // for now, nothing to do... except let's find the root element
        while (sr.next() != XMLStreamConstants.START_ELEMENT) {
            ;
        }
        return sr;
    }

    /*
    /**********************************************************************
    /* Internal methods, format auto-detection
    /**********************************************************************
     */

    private final static byte UTF8_BOM_1 = (byte) 0xEF;
    private final static byte UTF8_BOM_2 = (byte) 0xBB;
    private final static byte UTF8_BOM_3 = (byte) 0xBF;

    private final static byte BYTE_x = (byte) 'x';
    private final static byte BYTE_m = (byte) 'm';
    private final static byte BYTE_l = (byte) 'l';
    private final static byte BYTE_D = (byte) 'D';

    private final static byte BYTE_LT = (byte) '<';
    private final static byte BYTE_QMARK = (byte) '?';
    private final static byte BYTE_EXCL = (byte) '!';
    private final static byte BYTE_HYPHEN = (byte) '-';
    
    /**
     * Method that tries to figure out if content seems to be in some kind
     * of XML format.
     * Note that implementation here is not nearly as robust as what underlying
     * Stax parser will do; the idea is to first support common encodings,
     * then expand as needed (for example, it is not all that hard to support
     * UTF-16; but it is some work and not needed quite yet)
     */
    public static MatchStrength hasXMLFormat(InputAccessor acc) throws IOException
    {
        /* Basically we just need to find "<!", "<?" or "<NAME"... but ideally
         * we would actually see the XML declaration
         */
        if (!acc.hasMoreBytes()) {
            return MatchStrength.INCONCLUSIVE;
        }
        byte b = acc.nextByte();
        // Very first thing, a UTF-8 BOM? (later improvements: other BOM's, heuristics)
        if (b == UTF8_BOM_1) { // yes, looks like UTF-8 BOM
            if (!acc.hasMoreBytes()) {
                return MatchStrength.INCONCLUSIVE;
            }
            if (acc.nextByte() != UTF8_BOM_2) {
                return MatchStrength.NO_MATCH;
            }
            if (!acc.hasMoreBytes()) {
                return MatchStrength.INCONCLUSIVE;
            }
            if (acc.nextByte() != UTF8_BOM_3) {
                return MatchStrength.NO_MATCH;
            }
            if (!acc.hasMoreBytes()) {
                return MatchStrength.INCONCLUSIVE;
            }
            b = acc.nextByte();
        }
        // otherwise: XML declaration?
        boolean maybeXmlDecl = (b == BYTE_LT);
        if (!maybeXmlDecl) {
            int ch = skipSpace(acc, b);
            if (ch < 0) {
                return MatchStrength.INCONCLUSIVE;
            }
            b = (byte) ch;
            // If we did not get an LT, shouldn't be valid XML (minus encoding issues etc)
            if (b != BYTE_LT) {
                return MatchStrength.NO_MATCH;
            }
        }
        if (!acc.hasMoreBytes()) {
            return MatchStrength.INCONCLUSIVE;
        }
        b = acc.nextByte();
        // Couple of choices here
        if (b == BYTE_QMARK) { // <?
            b = acc.nextByte();
            if (b == BYTE_x) {
                if (maybeXmlDecl) {
                    if (acc.hasMoreBytes() && acc.nextByte() == BYTE_m) {
                        if (acc.hasMoreBytes() && acc.nextByte() == BYTE_l) {
                            return MatchStrength.FULL_MATCH;
                        }
                    }
                }
                // but even with just partial match, we ought to be fine
                return MatchStrength.SOLID_MATCH;
            }
            // Ok to start with some other char too; just not xml declaration
            if (validXmlNameStartChar(acc, b)) {
                return MatchStrength.SOLID_MATCH;
            }
        } else if (b == BYTE_EXCL) {
            /* must be <!-- comment --> or <!DOCTYPE ...>, since
             * <![CDATA[ ]]> can NOT come outside of root
             */
            if (!acc.hasMoreBytes()) {
                return MatchStrength.INCONCLUSIVE;
            }
            b = acc.nextByte();
            if (b == BYTE_HYPHEN) {
                if (!acc.hasMoreBytes()) {
                    return MatchStrength.INCONCLUSIVE;
                }
                if (acc.nextByte() == BYTE_HYPHEN) {
                    return MatchStrength.SOLID_MATCH;
                }
            } else if (b == BYTE_D) {
                return tryMatch(acc, "OCTYPE", MatchStrength.SOLID_MATCH);
            }
        } else {
            // maybe root element? Just needs to match first char.
            if (validXmlNameStartChar(acc, b)) {
                return MatchStrength.SOLID_MATCH;
            }
        }
        return MatchStrength.NO_MATCH;
    }

    private final static boolean validXmlNameStartChar(InputAccessor acc, byte b)
        throws IOException
    {
        /* Can make it actual real XML check in future; for now we do just crude
         * check for ASCII range
         */
        int ch = (int) b & 0xFF;
        if (ch >= 'A') { // in theory, colon could be; in practice it should never be valid (wrt namespace)
            // This is where we'd check for multi-byte UTF-8 chars (or whatever encoding is in use)...
            return true;
        }
        return false;
    }
    
    private final static MatchStrength tryMatch(InputAccessor acc, String matchStr, MatchStrength fullMatchStrength)
        throws IOException
    {
        for (int i = 0, len = matchStr.length(); i < len; ++i) {
            if (!acc.hasMoreBytes()) {
                return MatchStrength.INCONCLUSIVE;
            }
            if (acc.nextByte() != matchStr.charAt(i)) {
                return MatchStrength.NO_MATCH;
            }
        }
        return fullMatchStrength;
    }
    
    private final static int skipSpace(InputAccessor acc, byte b) throws IOException
    {
        while (true) {
            int ch = (int) b & 0xFF;
            if (!(ch == ' ' || ch == '\r' || ch == '\n' || ch == '\t')) {
                return ch;
            }
            if (!acc.hasMoreBytes()) {
                return -1;
            }
            b = acc.nextByte();
            ch = (int) b & 0xFF;
        }
    }

}