import com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey
import com.cloudbees.plugins.credentials.Credentials
import com.cloudbees.plugins.credentials.CredentialsProvider
import com.cloudbees.plugins.credentials.CredentialsScope
import com.cloudbees.plugins.credentials.SystemCredentialsProvider
import com.cloudbees.plugins.credentials.domains.Domain
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl
import hudson.plugins.git.BranchSpec
import hudson.plugins.git.GitSCM
import hudson.plugins.git.SubmoduleConfig
import hudson.plugins.git.UserRemoteConfig
import hudson.plugins.git.extensions.GitSCMExtension
import hudson.security.HudsonPrivateSecurityRealm
import hudson.security.FullControlOnceLoggedInAuthorizationStrategy
import hudson.util.Secret
import java.util.Collections
import jenkins.model.Jenkins
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl
import org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition
import org.jenkinsci.plugins.workflow.job.WorkflowJob

def jenkins = Jenkins.get()

def adminId = System.getenv('JENKINS_ADMIN_ID') ?: 'admin'
def adminPassword = System.getenv('JENKINS_ADMIN_PASSWORD')
if (adminPassword?.trim()) {
    def realm = new HudsonPrivateSecurityRealm(false)
    if (realm.getUser(adminId) == null) {
        realm.createAccount(adminId, adminPassword)
    }
    jenkins.setSecurityRealm(realm)

    def strategy = new FullControlOnceLoggedInAuthorizationStrategy()
    strategy.setAllowAnonymousRead(false)
    jenkins.setAuthorizationStrategy(strategy)
}

def store = jenkins.getExtensionList(SystemCredentialsProvider.class)[0].getStore()

def credentialExists = { String id ->
    CredentialsProvider.lookupCredentials(Credentials.class, jenkins, null, null).any { it.id == id }
}

def dbPassword = System.getenv('BACKEND_DB_PASSWORD')
if (dbPassword?.trim() && !credentialExists('backend-db-password')) {
    store.addCredentials(
            Domain.global(),
            new StringCredentialsImpl(
                    CredentialsScope.GLOBAL,
                    'backend-db-password',
                    'Development PostgreSQL password for backend deploy',
                    Secret.fromString(dbPassword)
            )
    )
}

def githubToken = System.getenv('GITHUB_TOKEN')
if (githubToken?.trim() && !credentialExists('github-token')) {
    store.addCredentials(
            Domain.global(),
            new UsernamePasswordCredentialsImpl(
                    CredentialsScope.GLOBAL,
                    'github-token',
                    'GitHub token for sweetnsweat/backend checkout',
                    'x-access-token',
                    githubToken
            )
    )
}

def deployKeyPath = System.getenv('BACKEND_DEPLOY_KEY_PATH') ?: '/var/jenkins_home/secrets/sweetnsweat_backend_jenkins'
if (new File(deployKeyPath).exists() && !credentialExists('sweetnsweat-backend-deploy-key')) {
    store.addCredentials(
            Domain.global(),
            new BasicSSHUserPrivateKey(
                    CredentialsScope.GLOBAL,
                    'sweetnsweat-backend-deploy-key',
                    'git',
                    new BasicSSHUserPrivateKey.FileOnMasterPrivateKeySource(deployKeyPath),
                    '',
                    'Read-only GitHub deploy key for sweetnsweat/backend'
            )
    )
}

def repoUrl = System.getenv('BACKEND_REPO_URL') ?: 'https://github.com/sweetnsweat/backend.git'
def scmCredentialId = credentialExists('github-token') ? 'github-token' : 'sweetnsweat-backend-deploy-key'
def jobName = 'sweetnsweat-backend-main'
def job = jenkins.getItem(jobName)
if (job == null) {
    job = jenkins.createProject(WorkflowJob.class, jobName)
}

def scm = new GitSCM(
        [new UserRemoteConfig(repoUrl, null, null, scmCredentialId)],
        [new BranchSpec('*/main')],
        false,
        Collections.<SubmoduleConfig>emptyList(),
        null,
        null,
        Collections.<GitSCMExtension>emptyList()
)
def definition = new CpsScmFlowDefinition(scm, 'Jenkinsfile')
definition.setLightweight(true)
job.setDefinition(definition)
job.save()

jenkins.save()
