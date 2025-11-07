package com.codevision.codevisionbackend.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * LoggerInsight
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-07T00:31:52.729797-05:00[America/New_York]", comments = "Generator version: 7.5.0")
public class LoggerInsight {

  private String className;

  private String filePath;

  private String logLevel;

  private Integer lineNumber;

  private String messageTemplate;

  @Valid
  private List<String> variables = new ArrayList<>();

  private Boolean piiRisk;

  private Boolean pciRisk;

  public LoggerInsight() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public LoggerInsight(String className, String logLevel) {
    this.className = className;
    this.logLevel = logLevel;
  }

  public LoggerInsight className(String className) {
    this.className = className;
    return this;
  }

  /**
   * Fully qualified class declaring the log statement.
   * @return className
  */
  @NotNull 
  @Schema(name = "className", description = "Fully qualified class declaring the log statement.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("className")
  public String getClassName() {
    return className;
  }

  public void setClassName(String className) {
    this.className = className;
  }

  public LoggerInsight filePath(String filePath) {
    this.filePath = filePath;
    return this;
  }

  /**
   * Source file path relative to the repository.
   * @return filePath
  */
  
  @Schema(name = "filePath", description = "Source file path relative to the repository.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("filePath")
  public String getFilePath() {
    return filePath;
  }

  public void setFilePath(String filePath) {
    this.filePath = filePath;
  }

  public LoggerInsight logLevel(String logLevel) {
    this.logLevel = logLevel;
    return this;
  }

  /**
   * Log level (INFO, WARN, ERROR, DEBUG, TRACE).
   * @return logLevel
  */
  @NotNull 
  @Schema(name = "logLevel", description = "Log level (INFO, WARN, ERROR, DEBUG, TRACE).", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("logLevel")
  public String getLogLevel() {
    return logLevel;
  }

  public void setLogLevel(String logLevel) {
    this.logLevel = logLevel;
  }

  public LoggerInsight lineNumber(Integer lineNumber) {
    this.lineNumber = lineNumber;
    return this;
  }

  /**
   * Line number reported by the parser.
   * @return lineNumber
  */
  
  @Schema(name = "lineNumber", description = "Line number reported by the parser.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("lineNumber")
  public Integer getLineNumber() {
    return lineNumber;
  }

  public void setLineNumber(Integer lineNumber) {
    this.lineNumber = lineNumber;
  }

  public LoggerInsight messageTemplate(String messageTemplate) {
    this.messageTemplate = messageTemplate;
    return this;
  }

  /**
   * String literal or expression passed to the logger.
   * @return messageTemplate
  */
  
  @Schema(name = "messageTemplate", description = "String literal or expression passed to the logger.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("messageTemplate")
  public String getMessageTemplate() {
    return messageTemplate;
  }

  public void setMessageTemplate(String messageTemplate) {
    this.messageTemplate = messageTemplate;
  }

  public LoggerInsight variables(List<String> variables) {
    this.variables = variables;
    return this;
  }

  public LoggerInsight addVariablesItem(String variablesItem) {
    if (this.variables == null) {
      this.variables = new ArrayList<>();
    }
    this.variables.add(variablesItem);
    return this;
  }

  /**
   * Arguments passed to the logger.
   * @return variables
  */
  
  @Schema(name = "variables", description = "Arguments passed to the logger.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("variables")
  public List<String> getVariables() {
    return variables;
  }

  public void setVariables(List<String> variables) {
    this.variables = variables;
  }

  public LoggerInsight piiRisk(Boolean piiRisk) {
    this.piiRisk = piiRisk;
    return this;
  }

  /**
   * True if the log statement potentially includes PII data.
   * @return piiRisk
  */
  
  @Schema(name = "piiRisk", description = "True if the log statement potentially includes PII data.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("piiRisk")
  public Boolean getPiiRisk() {
    return piiRisk;
  }

  public void setPiiRisk(Boolean piiRisk) {
    this.piiRisk = piiRisk;
  }

  public LoggerInsight pciRisk(Boolean pciRisk) {
    this.pciRisk = pciRisk;
    return this;
  }

  /**
   * True if the log statement potentially includes PCI data.
   * @return pciRisk
  */
  
  @Schema(name = "pciRisk", description = "True if the log statement potentially includes PCI data.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("pciRisk")
  public Boolean getPciRisk() {
    return pciRisk;
  }

  public void setPciRisk(Boolean pciRisk) {
    this.pciRisk = pciRisk;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LoggerInsight loggerInsight = (LoggerInsight) o;
    return Objects.equals(this.className, loggerInsight.className) &&
        Objects.equals(this.filePath, loggerInsight.filePath) &&
        Objects.equals(this.logLevel, loggerInsight.logLevel) &&
        Objects.equals(this.lineNumber, loggerInsight.lineNumber) &&
        Objects.equals(this.messageTemplate, loggerInsight.messageTemplate) &&
        Objects.equals(this.variables, loggerInsight.variables) &&
        Objects.equals(this.piiRisk, loggerInsight.piiRisk) &&
        Objects.equals(this.pciRisk, loggerInsight.pciRisk);
  }

  @Override
  public int hashCode() {
    return Objects.hash(className, filePath, logLevel, lineNumber, messageTemplate, variables, piiRisk, pciRisk);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LoggerInsight {\n");
    sb.append("    className: ").append(toIndentedString(className)).append("\n");
    sb.append("    filePath: ").append(toIndentedString(filePath)).append("\n");
    sb.append("    logLevel: ").append(toIndentedString(logLevel)).append("\n");
    sb.append("    lineNumber: ").append(toIndentedString(lineNumber)).append("\n");
    sb.append("    messageTemplate: ").append(toIndentedString(messageTemplate)).append("\n");
    sb.append("    variables: ").append(toIndentedString(variables)).append("\n");
    sb.append("    piiRisk: ").append(toIndentedString(piiRisk)).append("\n");
    sb.append("    pciRisk: ").append(toIndentedString(pciRisk)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

