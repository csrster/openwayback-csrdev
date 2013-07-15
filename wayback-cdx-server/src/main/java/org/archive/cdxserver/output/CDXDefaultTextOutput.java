package org.archive.cdxserver.output;

import java.io.PrintWriter;

import org.archive.format.cdx.CDXLine;

public class CDXDefaultTextOutput implements CDXOutput {

    @Override
    public void begin(PrintWriter writer) {
        // TODO Auto-generated method stub
    }

    @Override
    public void writeLine(PrintWriter writer, CDXLine line) {
        writer.println(line.fullLine);
    }

    @Override
    public void end(PrintWriter writer) {
        // TODO Auto-generated method stub
    }

    @Override
    public void writeResumeKey(PrintWriter writer, String resumeKey) {
        writer.println();
        writer.println(resumeKey);
    }

}