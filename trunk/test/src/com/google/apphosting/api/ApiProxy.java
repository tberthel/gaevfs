/*******************************************************************************
 * 
 * This code modified from the original to declare environmentThreadLocal to be
 * of type InheritableThreadLocal instead of ThreadLocal. See the following:
 * 
 *      http://code.google.com/p/googleappengine/issues/detail?id=2201
 *      
 * This is only needed for junit testing.
 *      
 *******************************************************************************/
/*     */ package com.google.apphosting.api;
/*     */ 
/*     */ import java.util.Map;
/*     */ import java.util.concurrent.Future;
/*     */ import java.util.concurrent.TimeUnit;
/*     */ 
/*     */ public class ApiProxy
/*     */ {
/*  24 */   private static final InheritableThreadLocal<Environment> environmentThreadLocal = new InheritableThreadLocal<Environment>();
/*     */   private static Delegate<Environment> delegate;
/*     */ 
/*     */   public static byte[] makeSyncCall(String packageName, String methodName, byte[] request)
/*     */     throws ApiProxy.ApiProxyException
/*     */   {
/*  76 */     Environment env = getCurrentEnvironment();
/*  77 */     if ((delegate == null) || (env == null))
/*     */     {
/*  81 */       throw new CallNotFoundException(packageName, methodName);
/*     */     }
/*  83 */     return delegate.makeSyncCall(env, packageName, methodName, request);
/*     */   }
/*     */ 
/*     */   public static Future<byte[]> makeAsyncCall(final String packageName, final String methodName, byte[] request, ApiConfig apiConfig)
/*     */   {
/* 118 */     Environment env = getCurrentEnvironment();
/* 119 */     if ((delegate == null) || (env == null))
/*     */     {
/* 123 */       return new Future<byte[]>() {
/*     */         public byte[] get() {
/* 125 */           throw new ApiProxy.CallNotFoundException(packageName, methodName);
/*     */         }
/*     */ 
/*     */         public byte[] get(long deadline, TimeUnit unit) {
/* 129 */           throw new ApiProxy.CallNotFoundException(packageName, methodName);
/*     */         }
/*     */ 
/*     */         public boolean isDone() {
/* 133 */           return true;
/*     */         }
/*     */ 
/*     */         public boolean isCancelled() {
/* 137 */           return false;
/*     */         }
/*     */ 
/*     */         public boolean cancel(boolean shouldInterrupt) {
/* 141 */           return false;
/*     */         }
/*     */       };
/*     */     }
/* 145 */     return delegate.makeAsyncCall(env, packageName, methodName, request, apiConfig);
/*     */   }
/*     */ 
/*     */   public static void log(LogRecord record)
/*     */   {
/* 150 */     if (delegate != null)
/* 151 */       delegate.log(getCurrentEnvironment(), record);
/*     */   }
/*     */ 
/*     */   public static Environment getCurrentEnvironment()
/*     */   {
/* 160 */     return environmentThreadLocal.get();
/*     */   }
/*     */ 
/*     */   public static void setDelegate(Delegate<Environment> aDelegate)
/*     */   {
/* 168 */     delegate = aDelegate;
/*     */   }
/*     */ 
/*     */   public static Delegate<Environment> getDelegate()
/*     */   {
/* 178 */     return delegate;
/*     */   }
/*     */ 
/*     */   public static void setEnvironmentForCurrentThread(Environment environment)
/*     */   {
/* 191 */     environmentThreadLocal.set(environment);
/*     */   }
/*     */ 
/*     */   public static void clearEnvironmentForCurrentThread()
/*     */   {
/* 199 */     environmentThreadLocal.set(null);
/*     */   }
/*     */ 
/*     */   @SuppressWarnings("serial")
/*     */   public static class UnknownException extends ApiProxy.ApiProxyException
/*     */   {
/*     */     public UnknownException(String packageName, String methodName, Throwable nestedException)
/*     */     {
/* 507 */       super("An error occurred for the API request %s.%s().", packageName, methodName, nestedException);
/*     */     }
/*     */ 
/*     */     public UnknownException(String packageName, String methodName)
/*     */     {
/* 512 */       super("An error occurred for the API request %s.%s().", packageName, methodName);
/*     */     }
/*     */ 
/*     */     public UnknownException(String message)
/*     */     {
/* 517 */       super(message);
/*     */     }
/*     */   }
/*     */ 
/*     */   @SuppressWarnings("serial")
/*     */   public static class RequestTooLargeException extends ApiProxy.ApiProxyException
/*     */   {
/*     */     public RequestTooLargeException(String packageName, String methodName)
/*     */     {
/* 496 */       super("The request to API call %s.%s() was too large.", packageName, methodName);
/*     */     }
/*     */   }
/*     */ 
/*     */   @SuppressWarnings("serial")
/*     */   public static class OverQuotaException extends ApiProxy.ApiProxyException
/*     */   {
/*     */     public OverQuotaException(String packageName, String methodName)
/*     */     {
/* 489 */       super("The API call %s.%s() required more quota than is unavailable.", packageName, methodName);
/*     */     }
/*     */   }
/*     */ 
/*     */   @SuppressWarnings("serial")
/*     */   public static class FeatureNotEnabledException extends ApiProxy.ApiProxyException
/*     */   {
/*     */     public FeatureNotEnabledException(String message, String packageName, String methodName)
/*     */     {
/* 483 */       super(message, packageName, methodName);
/*     */     }
/*     */   }
/*     */ 
/*     */   @SuppressWarnings("serial")
/*     */   public static class CapabilityDisabledException extends ApiProxy.ApiProxyException
/*     */   {
/*     */     public CapabilityDisabledException(String packageName, String methodName)
/*     */     {
/* 474 */       super("The API call %s.%s() is temporarily unavailable.", packageName, methodName);
/*     */     }
/*     */   }
/*     */ 
/*     */   @SuppressWarnings("serial")
/*     */   public static class CancelledException extends ApiProxy.ApiProxyException
/*     */   {
/*     */     public CancelledException(String packageName, String methodName)
/*     */     {
/* 467 */       super("The API call %s.%s() was explicitly cancelled.", packageName, methodName);
/*     */     }
/*     */   }
/*     */ 
/*     */   @SuppressWarnings("serial")
/*     */   public static class ApiDeadlineExceededException extends ApiProxy.ApiProxyException
/*     */   {
/*     */     public ApiDeadlineExceededException(String packageName, String methodName)
/*     */     {
/* 460 */       super("The API call %s.%s() took too long to respond and was cancelled.", packageName, methodName);
/*     */     }
/*     */   }
/*     */ 
/*     */   @SuppressWarnings("serial")
/*     */   public static class ArgumentException extends ApiProxy.ApiProxyException
/*     */   {
/*     */     public ArgumentException(String packageName, String methodName)
/*     */     {
/* 452 */       super("An error occurred parsing (locally or remotely) the arguments to %S.%s().", packageName, methodName);
/*     */     }
/*     */   }
/*     */ 
/*     */   @SuppressWarnings("serial")
/*     */   public static class CallNotFoundException extends ApiProxy.ApiProxyException
/*     */   {
/*     */     public CallNotFoundException(String packageName, String methodName)
/*     */     {
/* 445 */       super("The API package '%s' or call '%s()' was not found.", packageName, methodName);
/*     */     }
/*     */   }
/*     */ 
/*     */   @SuppressWarnings("serial")
/*     */   public static class RPCFailedException extends ApiProxy.ApiProxyException
/*     */   {
/*     */     public RPCFailedException(String packageName, String methodName)
/*     */     {
/* 437 */       super("The remote RPC to the application server failed for the call %s.%s().", packageName, methodName);
/*     */     }
/*     */   }
/*     */ 
/*     */   @SuppressWarnings("serial")
/*     */   public static class ApplicationException extends ApiProxy.ApiProxyException
/*     */   {
/*     */     private final int applicationError;
/*     */     private final String errorDetail;
/*     */ 
/*     */     public ApplicationException(int applicationError)
/*     */     {
/* 417 */       this(applicationError, "");
/*     */     }
/*     */ 
/*     */     public ApplicationException(int applicationError, String errorDetail) {
/* 421 */       super("ApplicationError: " + applicationError + ": " + errorDetail);
/* 422 */       this.applicationError = applicationError;
/* 423 */       this.errorDetail = errorDetail;
/*     */     }
/*     */ 
/*     */     public int getApplicationError() {
/* 427 */       return this.applicationError;
/*     */     }
/*     */ 
/*     */     public String getErrorDetail() {
/* 431 */       return this.errorDetail;
/*     */     }
/*     */   }
/*     */ 
/*     */   @SuppressWarnings("serial")
/*     */   public static class ApiProxyException extends RuntimeException
/*     */   {
/*     */     public ApiProxyException(String message, String packageName, String methodName)
/*     */     {
/* 399 */       this(String.format(message, new Object[] { packageName, methodName }));
/*     */     }
/*     */ 
/*     */     private ApiProxyException(String message, String packageName, String methodName, Throwable nestedException)
/*     */     {
/* 404 */       super(String.format(message, new Object[] { packageName, methodName }), nestedException);
/*     */     }
/*     */ 
/*     */     public ApiProxyException(String message) {
/* 408 */       super(message);
/*     */     }
/*     */   }
/*     */ 
/*     */   public static final class ApiConfig
/*     */   {
/*     */     private Double deadlineInSeconds;
/*     */ 
/*     */     public Double getDeadlineInSeconds()
/*     */     {
/* 383 */       return this.deadlineInSeconds;
/*     */     }
/*     */ 
/*     */     public void setDeadlineInSeconds(Double deadlineInSeconds)
/*     */     {
/* 391 */       this.deadlineInSeconds = deadlineInSeconds;
/*     */     }
/*     */   }
/*     */ 
/*     */   public static final class LogRecord
/*     */   {
/*     */     private final Level level;
/*     */     private final long timestamp;
/*     */     private final String message;
/*     */ 
/*     */     public LogRecord(Level level, long timestamp, String message)
/*     */     {
/* 350 */       this.level = level;
/* 351 */       this.timestamp = timestamp;
/* 352 */       this.message = message;
/*     */     }
/*     */ 
/*     */     public Level getLevel() {
/* 356 */       return this.level;
/*     */     }
/*     */ 
/*     */     public long getTimestamp()
/*     */     {
/* 363 */       return this.timestamp;
/*     */     }
/*     */ 
/*     */     public String getMessage() {
/* 367 */       return this.message;
/*     */     }
/*     */ 
/*     */     public static enum Level
/*     */     {
/* 341 */       debug, info, warn, error, fatal;
/*     */     }
/*     */   }
/*     */ 
/*     */   public static abstract interface Delegate<E extends ApiProxy.Environment>
/*     */   {
/*     */     public abstract byte[] makeSyncCall(E paramE, String paramString1, String paramString2, byte[] paramArrayOfByte)
/*     */       throws ApiProxy.ApiProxyException;
/*     */ 
/*     */     public abstract Future<byte[]> makeAsyncCall(E paramE, String paramString1, String paramString2, byte[] paramArrayOfByte, ApiProxy.ApiConfig paramApiConfig);
/*     */ 
/*     */     public abstract void log(E paramE, ApiProxy.LogRecord paramLogRecord);
/*     */   }
/*     */ 
/*     */   public static abstract interface Environment
/*     */   {
/*     */     public abstract String getAppId();
/*     */ 
/*     */     public abstract String getVersionId();
/*     */ 
/*     */     public abstract String getEmail();
/*     */ 
/*     */     public abstract boolean isLoggedIn();
/*     */ 
/*     */     public abstract boolean isAdmin();
/*     */ 
/*     */     public abstract String getAuthDomain();
/*     */ 
/*     */     public abstract String getRequestNamespace();
/*     */ 
/*     */     public abstract Map<String, Object> getAttributes();
/*     */   }
/*     */ }

// Location:           C:\appengine-java-sdk-1.2.8\lib\shared\appengine-local-runtime-shared.jar
// Qualified Name:     com.google.apphosting.api.ApiProxy
// Java Class Version: 5 (49.0)
// JD-Core Version:    0.5.1

