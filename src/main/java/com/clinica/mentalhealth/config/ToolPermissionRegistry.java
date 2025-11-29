package com.clinica.mentalhealth.config;

import com.clinica.mentalhealth.domain.Role;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;

/**
 * Registro centralizado de herramientas de IA y sus permisos por rol.
 * Elimina la lógica manual de filtrado en ClinicalAgentService.
 */
@Configuration
public class ToolPermissionRegistry {

  private final Map<String, Set<Role>> toolPermissions = new HashMap<>();

  /**
   * Escanea los beans de tipo Function y extrae sus permisos desde @AllowedRoles.
   */
  @Bean
  public ToolPermissionRegistry toolPermissionRegistryBean(ListableBeanFactory beanFactory) {
    // Obtener todos los beans definidos en AiToolsConfig
    String[] beanNames = beanFactory.getBeanNamesForType(Function.class);

    for (String beanName : beanNames) {
      try {
        // Buscar el método @Bean que define esta función
        Method[] methods = AiToolsConfig.class.getDeclaredMethods();
        for (Method method : methods) {
          if (method.getName().equals(beanName)) {
            AllowedRoles annotation = AnnotationUtils.findAnnotation(method, AllowedRoles.class);
            if (annotation != null) {
              toolPermissions.put(beanName, EnumSet.copyOf(Arrays.asList(annotation.value())));
            }
            break;
          }
        }
      } catch (Exception e) {
        // Ignorar beans que no son tools
      }
    }

    return this;
  }

  /**
   * Obtiene las herramientas permitidas para un rol específico.
   */
  public Set<String> getToolsForRole(String roleName) {
    Role role = Role.valueOf(roleName);
    Set<String> allowed = new HashSet<>();

    for (var entry : toolPermissions.entrySet()) {
      if (entry.getValue().contains(role)) {
        allowed.add(entry.getKey());
      }
    }

    return allowed;
  }

  /**
   * Obtiene las herramientas permitidas para múltiples roles.
   */
  public Set<String> getToolsForRoles(Set<Role> roles) {
    Set<String> allowed = new HashSet<>();

    for (var entry : toolPermissions.entrySet()) {
      for (Role role : roles) {
        if (entry.getValue().contains(role)) {
          allowed.add(entry.getKey());
          break;
        }
      }
    }

    return allowed;
  }

  /**
   * Registra manualmente una herramienta y sus roles permitidos.
   * Útil para herramientas definidas fuera de AiToolsConfig.
   */
  public void registerTool(String toolName, Role... roles) {
    toolPermissions.put(toolName, EnumSet.copyOf(Arrays.asList(roles)));
  }

  /**
   * Verifica si un rol puede usar una herramienta específica.
   */
  public boolean canUse(String toolName, Role role) {
    Set<Role> allowed = toolPermissions.get(toolName);
    return allowed != null && allowed.contains(role);
  }
}
