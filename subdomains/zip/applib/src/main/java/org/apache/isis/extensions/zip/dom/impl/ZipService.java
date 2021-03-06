package org.apache.isis.extensions.zip.dom.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.google.common.io.ByteSource;
import com.google.common.io.Files;

import org.springframework.stereotype.Service;

import org.apache.isis.applib.FatalException;

import lombok.Data;

@Service
public class ZipService {

    @Data
    public static class FileAndName {
        private final String name;
        private final File file;
    }

    @Deprecated
    public byte[] zip(final List<FileAndName> fileAndNameList) {
        return zipNamedFiles(fileAndNameList);
    }

    /**
     * Rather than use the name of the file (which might be temporary files, for example)
     * we explicitly provide the name to use (in the ZipEntry).
     */
    public byte[] zipNamedFiles(final List<FileAndName> fileAndNameList) {

        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final ZipOutputStream zos = new ZipOutputStream(baos);

            for (final FileAndName fan : fileAndNameList) {
                zos.putNextEntry(new ZipEntry(fan.getName()));
                final ByteSource byteSource = Files.asByteSource(fan.getFile());
                zos.write(byteSource.read());
                zos.closeEntry();
            }
            zos.close();
            return baos.toByteArray();
        } catch (final IOException ex) {
            throw new FatalException("Unable to create zip", ex);
        }
    }

    /**
     * As per {@link #zip(List)}, but using each file's name as the zip entry (rather than providing it).
     */
    public byte[] zipFiles(final List<File> fileList) {
        return zip(fileList.stream()
                           .map(file -> new FileAndName(file.getName(), file))
                           .collect(Collectors.toList())
                );
    }

    @Data
    public static class BytesAndName {
        private final String name;
        private final byte[] bytes;
    }

    /**
     * Similar to {@link #zipNamedFiles(List)}, but uses simple byte[] as the input, rather than files.
     *
     * @param bytesAndNameList
     * @return
     */
    public byte[] zipNamedBytes(final List<BytesAndName> bytesAndNameList) {

        final byte[] bytes;
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final ZipOutputStream zos = new ZipOutputStream(baos);

            for (final BytesAndName ban : bytesAndNameList) {
                zos.putNextEntry(new ZipEntry(ban.getName()));
                zos.write(ban.getBytes());
                zos.closeEntry();
            }
            zos.close();
            bytes = baos.toByteArray();
        } catch (final IOException ex) {
            throw new FatalException("Unable to create zip", ex);
        }
        return bytes;
    }

}
