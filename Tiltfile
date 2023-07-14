# Build
custom_build(
    # Name of the container image
    ref = 'gamecloud-order-service',
    # Command to build the container image
    command = './gradlew bootBuildImage --imageName $EXPECTED_REF',
    # Files to watch that trigger a new build
    deps = ['build.gradle', 'src']
)

# Deploy
k8s_yaml([
	'dev/k8s/deployment.yaml',
	'dev/k8s/service.yaml'
])

# Manage
k8s_resource('order-service', port_forwards=['9002'])
