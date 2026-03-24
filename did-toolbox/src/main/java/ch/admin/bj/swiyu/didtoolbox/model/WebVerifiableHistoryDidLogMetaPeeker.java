package ch.admin.bj.swiyu.didtoolbox.model;

import ch.admin.bj.swiyu.didtoolbox.TdwUpdater;
import ch.admin.eid.did_sidekicks.DidDoc;
import ch.admin.eid.did_sidekicks.DidMethodParameter;
import ch.admin.eid.didresolver.Did;
import ch.admin.eid.didresolver.DidResolveException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import lombok.Getter;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A quite rudimentary did:webvh DID log entry parser intended as a sidekick (helper) of {@link TdwUpdater}.
 */
public final class WebVerifiableHistoryDidLogMetaPeeker {

    private WebVerifiableHistoryDidLogMetaPeeker() {
    }

    static class WebVhDidLogEntry {
        String versionId;
        String versionTime;

        // Skip parsing the "parameters", as they will be supplied by the resolver afterwards

        @Getter
        @SerializedName("state")
        DidDocument didDocument;

        // Skip "DataIntegrityProof" as irrelevant in this context
    }

    /**
     * The essential method oh the helper class.
     *
     * @param didLog to peek into. It is assumed a "resolvable" {@link DidMethodEnum#WEBVH_1_0}-conform DID log is supplied.
     * @return metadata describing a DID log (to a certain extent).
     * @throws DidLogMetaPeekerException if "peeking" failed for whatever reason.
     *                                   The {@link MalformedWebVerifiableHistoryDidLogMetaPeekerException} variant
     *                                   if thrown in case a fully malformed DID log (in terms of specification) was supplied
     */
    @SuppressWarnings({"PMD.CyclomaticComplexity"})
    public static DidLogMeta peek(String didLog) throws DidLogMetaPeekerException {

        var normalizedLog = normalize(didLog);

        AtomicReference<Exception> jsonSyntaxEx = new AtomicReference<>();
        AtomicReference<String> lastVersionId = new AtomicReference<>();
        AtomicReference<String> dateTime = new AtomicReference<>();
        AtomicReference<String> didDocId = new AtomicReference<>();

        Gson gson = new Gson();
        try (JsonReader reader = new JsonReader(new StringReader(normalizedLog))) {
            reader.setLenient(true);

            while (reader.peek() != JsonToken.END_DOCUMENT) {
                WebVhDidLogEntry entry = gson.fromJson(reader, WebVhDidLogEntry.class);

                if (entry != null) {
                    lastVersionId.set(entry.versionId);
                    dateTime.set(entry.versionTime);

                    // Skip parsing the parameters, as they will be supplied by the resolver afterwards

                    var didDocument = entry.getDidDocument();
                    if (didDocument != null && didDocument.getId() != null) {
                        didDocId.set(didDocument.getId());
                    }
                }
            }
        } catch (JsonSyntaxException e) {
            jsonSyntaxEx.set(e);
        } catch (IOException e) {
            // This should not happen with StringReader, but we must handle it
            throw new DidLogMetaPeekerException("Failed to read DID log", e);
        }

        if (jsonSyntaxEx.get() != null) {
            throw new MalformedWebVerifiableHistoryDidLogMetaPeekerException("Malformed " + DidMethodEnum.WEBVH_1_0.asString() + " log entry (a JSON object expected)", jsonSyntaxEx.get());
        }

        if (lastVersionId.get() == null) {
            throw new DidLogMetaPeekerException("Missing versionId");
        }

        var split = lastVersionId.get().split("-");
        if (split.length != 2) {
            throw new DidLogMetaPeekerException("Every versionId MUST be a dash-separated combination of version number and entry hash, found: " + lastVersionId.get());
        }
        int lastVersionNumber;
        try {
            lastVersionNumber = Integer.parseInt(split[0]);
        } catch (NumberFormatException e) {
            throw new DidLogMetaPeekerException("Invalid DID log entry version number: " + split[0], e);
        }

        if (dateTime.get() == null) {
            throw new DidLogMetaPeekerException("Missing versionTime");
        }

        if (dateTime.get().isEmpty()) {
            throw new DidLogMetaPeekerException("The versionTime MUST be a valid ISO8601 date/time string");
        }

        if (didDocId.get() == null) {
            throw new DidLogMetaPeekerException("Missing DID document");
        }

        DidDoc didDoc;
        Map<String, DidMethodParameter> didMethodParameters;
        try {
            var resolveAll = new Did(didDocId.get()).resolveAll(normalizedLog);
            didDoc = resolveAll.getDidDoc();
            didMethodParameters = resolveAll.getDidMethodParameters();
        } catch (DidResolveException e) {
            throw new DidLogMetaPeekerException(e);
        }

        return new DidLogMeta(lastVersionId.get(), lastVersionNumber, dateTime.get(), didMethodParameters, didDoc);
    }

    private static String normalize(String didLog) {
        if (didLog == null || didLog.trim().isEmpty()) {
            return didLog;
        }

        StringBuilder sb = new StringBuilder();
        Gson gson = new GsonBuilder().create();
        try (JsonReader reader = new JsonReader(new StringReader(didLog.trim()))) {
            reader.setLenient(true);
            while (reader.peek() != JsonToken.END_DOCUMENT) {
                Object entry = gson.fromJson(reader, Object.class);
                if (entry != null) {
                    sb.append(gson.toJson(entry)).append('\n');
                }
            }
        } catch (Exception e) {
            // If normalization fails, return the original and let the parser handle it
            return didLog;
        }
        return sb.toString();
    }
}
