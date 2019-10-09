/*
 * Copyright 2018 Google LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.opensource.dependencies;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.common.truth.IterableSubject;
import com.google.common.truth.Subject;
import com.google.common.truth.Truth;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionScheme;
import org.junit.Assert;
import org.junit.Test;

public class RepositoryUtilityTest {

  @Test
  public void testFindLocalRepository() {
    RepositorySystem system = RepositoryUtility.newRepositorySystem();
    RepositorySystemSession session = RepositoryUtility.newSession(system);
    
    File local = session.getLocalRepository().getBasedir();
    Assert.assertTrue(local.exists());
    Assert.assertTrue(local.canRead());
    Assert.assertTrue(local.canWrite());
  }
  
  @Test
  public void testReadBom_coordinates() throws ArtifactDescriptorException {
    Bom bom = RepositoryUtility.readBom("com.google.cloud:google-cloud-bom:0.61.0-alpha");
    List<Artifact> managedDependencies = bom.getManagedDependencies();
    // Characterization test. As long as the artifact doesn't change (and it shouldn't)
    // the answer won't change.
    Assert.assertEquals(134, managedDependencies.size());
    Assert.assertEquals("com.google.cloud:google-cloud-bom:0.61.0-alpha", bom.getCoordinates());
  }

  @Test
  public void testReadBom_path() throws MavenRepositoryException, ArtifactDescriptorException {
    Path pomFile = Paths.get("..", "boms", "cloud-oss-bom", "pom.xml");
    
    Bom currentBom = RepositoryUtility.readBom(pomFile);
    Bom oldBom = RepositoryUtility.readBom("com.google.cloud:libraries-bom:2.5.0");
    ImmutableList<Artifact> currentArtifacts = currentBom.getManagedDependencies();
    ImmutableList<Artifact> oldArtifacts = oldBom.getManagedDependencies();
    
    String coordinates = currentBom.getCoordinates();
    Truth.assertThat(coordinates).startsWith("com.google.cloud:libraries-bom:");
    Truth.assertThat(coordinates).endsWith("-SNAPSHOT");
    
    // This is a characterization test to verify that the managed dependencies haven't changed.
    // However sometimes this list does change. If so, we want to 
    // output the specific difference so we can manually verify whether
    // the changes make sense. When they do make sense, we update the test. 
    if (currentArtifacts.size() != 217) {
        Set<Artifact> current = Sets.newHashSet(currentArtifacts);
        Set<Artifact> old = Sets.newHashSet(oldArtifacts);
        SetView<Artifact> currentMinusOld = Sets.difference(current, old);
        String added = Joiner.on(", ").join(currentMinusOld);
        SetView<Artifact> oldMinusCurrent = Sets.difference(old, current);
        String subtracted = Joiner.on(", ").join(oldMinusCurrent);
        Assert.fail("Dependency tree changed.\n  Added " + added + "\n  Removed" + subtracted);
    }
  }

  @Test
  public void testFindVersions() throws MavenRepositoryException {
    RepositorySystem system = RepositoryUtility.newRepositorySystem();
    ImmutableList<String> versions =
        RepositoryUtility.findVersions(system, "com.google.cloud", "libraries-bom");
    Truth.assertThat(versions).containsAtLeast("1.1.0", "1.1.1", "1.2.0", "2.0.0").inOrder();
  }

  @Test
  public void testFindHighestVersions()
      throws MavenRepositoryException, InvalidVersionSpecificationException {
    RepositorySystem system = RepositoryUtility.newRepositorySystem();

    // FindHighestVersion should work for both jar and pom (extension:pom) artifacts
    for (String artifactId : ImmutableList.of("guava", "guava-bom")) {
      String guavaHighestVersion =
          RepositoryUtility.findHighestVersion(
              system, RepositoryUtility.newSession(system), "com.google.guava", artifactId);
      Assert.assertNotNull(guavaHighestVersion);

      // Not comparing alphabetically; otherwise "100.0" would be smaller than "28.0"
      VersionScheme versionScheme = new GenericVersionScheme();
      Version highestGuava = versionScheme.parseVersion(guavaHighestVersion);
      Version guava28 = versionScheme.parseVersion("28.0");

      Truth.assertWithMessage("Latest guava release is greater than or equal to 28.0")
          .that(highestGuava)
          .isAtLeast(guava28);
    }
  }
}
