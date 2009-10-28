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
/*     */ 
/*     */ public class ApiProxy
/*     */ {
/*  22 */   private static final InheritableThreadLocal<Environment> environmentThreadLocal = new InheritableThreadLocal<Environment>();
/*     */   private static Delegate<Environment> delegate;
/*     */ 
/*     */   public static byte[] makeSyncCall(String packageName, String methodName, byte[] request)
/*     */     throws ApiProxy.ApiProxyException
/*     */   {
/*  72 */     Environment env = getCurrentEnvironment();
/*  73 */     if ((delegate == null) || (env == null))
/*     */     {
/*  77 */       throw new CallNotFoundException(packageName, methodName);
/*     */     }
/*  79 */     return delegate.makeSyncCall(env, packageName, methodName, request);
/*     */   }
/*     */ 
/*     */   public static void log(LogRecord record)
/*     */   {
/*  84 */     if (delegate != null)
/*  85 */       delegate.log(getCurrentEnvironment(), record);
/*     */   }
/*     */ 
/*     */   public static Environment getCurrentEnvironment()
/*     */   {
/*  94 */     return environmentThreadLocal.get();
/*     */   }
/*     */ 
/*     */   public static void setDelegate(Delegate<Environment> aDelegate)
/*     */   {
/* 102 */     delegate = aDelegate;
/*     */   }
/*     */ 
/*     */   public static Delegate<Environment> getDelegate()
/*     */   {
/* 112 */     return delegate;
/*     */   }
/*     */ 
/*     */   public static void setEnvironmentForCurrentThread(Environment environment)
/*     */   {
/* 125 */     environmentThreadLocal.set(environment);
/*     */   }
/*     */ 
/*     */   public static void clearEnvironmentForCurrentThread()
/*     */   {
/* 133 */     environmentThreadLocal.set(null);
/*     */   }
/*     */ 
/*     */   public static class UnknownException extends ApiProxy.ApiProxyException
/*     */   {
/*     */     public UnknownException(String packageName, String methodName, Throwable nestedException)
/*     */     {
/* 381 */       super("An error occurred for the API request %s.%s().", packageName, methodName, nestedException);
/*     */     }
/*     */ 
/*     */     public UnknownException(String packageName, String methodName)
/*     */     {
/* 386 */       super("An error occurred for the API request %s.%s().", packageName, methodName);
/*     */     }
/*     */ 
/*     */     public UnknownException(String message)
/*     */     {
/* 391 */       super(message);
/*     */     }
/*     */   }
/*     */ 
/*     */   public static class RequestTooLargeException extends ApiProxy.ApiProxyException
/*     */   {
/*     */     public RequestTooLargeException(String packageName, String methodName)
/*     */     {
/* 370 */       super("The request to API call %s.%s() was too large.", packageName, methodName);
/*     */     }
/*     */   }
/*     */ 
/*     */   public static class OverQuotaException extends ApiProxy.ApiProxyException
/*     */   {
/*     */     public OverQuotaException(String packageName, String methodName)
/*     */     {
/* 363 */       super("The API call %s.%s() required more quota than is unavailable.", packageName, methodName);
/*     */     }
/*     */   }
/*     */ 
/*     */   public static class CapabilityDisabledException extends ApiProxy.ApiProxyException
/*     */   {
/*     */     public CapabilityDisabledException(String packageName, String methodName)
/*     */     {
/* 356 */       super("The API call %s.%s() is temporarily unavailable.", packageName, methodName);
/*     */     }
/*     */   }
/*     */ 
/*     */   public static class CancelledException extends ApiProxy.ApiProxyException
/*     */   {
/*     */     public CancelledException(String packageName, String methodName)
/*     */     {
/* 349 */       super("The API call %s.%s() was explicitly cancelled.", packageName, methodName);
/*     */     }
/*     */   }
/*     */ 
/*     */   public static class ApiDeadlineExceededException extends ApiProxy.ApiProxyException
/*     */   {
/*     */     public ApiDeadlineExceededException(String packageName, String methodName)
/*     */     {
/* 342 */       super("The API call %s.%s() took too long to respond and was cancelled.", packageName, methodName);
/*     */     }
/*     */   }
/*     */ 
/*     */   public static class ArgumentException extends ApiProxy.ApiProxyException
/*     */   {
/*     */     public ArgumentException(String packageName, String methodName)
/*     */     {
/* 334 */       super("An error occurred parsing (locally or remotely) the arguments to %S.%s().", packageName, methodName);
/*     */     }
/*     */   }
/*     */ 
/*     */   public static class CallNotFoundException extends ApiProxy.ApiProxyException
/*     */   {
/*     */     public CallNotFoundException(String packageName, String methodName)
/*     */     {
/* 327 */       super("The API package '%s' or call '%s()' was not found.", packageName, methodName);
/*     */     }
/*     */   }
/*     */ 
/*     */   public static class RPCFailedException extends ApiProxy.ApiProxyException
/*     */   {
/*     */     public RPCFailedException(String packageName, String methodName)
/*     */     {
/* 319 */       super("The remote RPC to the application server failed for the call %s.%s().", packageName, methodName);
/*     */     }
/*     */   }
/*     */ 
/*     */   public static class ApplicationException extends ApiProxy.ApiProxyException
/*     */   {
/*     */     private final int applicationError;
/*     */     private final String errorDetail;
/*     */ 
/*     */     public ApplicationException(int applicationError)
/*     */     {
/* 299 */       this(applicationError, "");
/*     */     }
/*     */ 
/*     */     public ApplicationException(int applicationError, String errorDetail) {
/* 303 */       super("ApplicationError: " + applicationError + ": " + errorDetail);
/* 304 */       this.applicationError = applicationError;
/* 305 */       this.errorDetail = errorDetail;
/*     */     }
/*     */ 
/*     */     public int getApplicationError() {
/* 309 */       return this.applicationError;
/*     */     }
/*     */ 
/*     */     public String getErrorDetail() {
/* 313 */       return this.errorDetail;
/*     */     }
/*     */   }
/*     */ 
/*     */   public static class ApiProxyException extends RuntimeException
/*     */   {
/*     */     public ApiProxyException(String message, String packageName, String methodName)
/*     */     {
/* 281 */       this(String.format(message, new Object[] { packageName, methodName }));
/*     */     }
/*     */ 
/*     */     private ApiProxyException(String message, String packageName, String methodName, Throwable nestedException)
/*     */     {
/* 286 */       super(String.format(message, new Object[] { packageName, methodName }), nestedException);
/*     */     }
/*     */ 
/*     */     public ApiProxyException(String message) {
/* 290 */       super(message);
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
/* 256 */       this.level = level;
/* 257 */       this.timestamp = timestamp;
/* 258 */       this.message = message;
/*     */     }
/*     */ 
/*     */     public Level getLevel() {
/* 262 */       return this.level;
/*     */     }
/*     */ 
/*     */     public long getTimestamp()
/*     */     {
/* 269 */       return this.timestamp;
/*     */     }
/*     */ 
/*     */     public String getMessage() {
/* 273 */       return this.message;
/*     */     }
/*     */ 
/*     */     public static enum Level
/*     */     {
/* 247 */       debug, info, warn, error, fatal;
/*     */     }
/*     */   }
/*     */ 
/*     */   public static abstract interface Delegate<E extends ApiProxy.Environment>
/*     */   {
/*     */     public abstract byte[] makeSyncCall(E paramE, String paramString1, String paramString2, byte[] paramArrayOfByte)
/*     */       throws ApiProxy.ApiProxyException;
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

// Location:           C:\Users\vinceb\eclipse-jee-galileo-SR1\plugins\com.google.appengine.eclipse.sdkbundle_1.2.6.v200910131704\appengine-java-sdk-1.2.6\lib\shared\appengine-local-runtime-shared.jar
// Qualified Name:     com.google.apphosting.api.ApiProxy
// Java Class Version: 5 (49.0)
// JD-Core Version:    0.5.1
