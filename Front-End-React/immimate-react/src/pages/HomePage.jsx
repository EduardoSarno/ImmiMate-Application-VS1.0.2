import React from 'react';
import { Link } from 'react-router-dom';
import Navbar from '../components/Navbar';
import Footer from '../components/Footer';

const HomePage = () => {
  return (
    <>
      <Navbar />
      
      <main className="home-content">
        {/* Hero Section */}
        <section className="hero-section">
          <div className="hero-text">
            <h1>Empower Your Journey to Canadian Permanent Residency</h1>
            <p>
              Unlock personalized immigration guidance, program analysis, and actionable
              steps with our AI-driven agent.
            </p>
            <div className="hero-buttons">
              <Link to="/signup" className="btn-primary">
                Enroll Now
              </Link>
              <button className="btn-secondary">
                Learn More
              </button>
            </div>
          </div>
          <div className="hero-image">
            <img src="/Images/hero-image.png" alt="Immigration AI Assistance" />
          </div>
        </section>

        {/* Features Section */}
        <section className="features-section">
          <h2 className="features-title">Key Features to Empower Your Journey</h2>
          <p className="features-subtitle">
            Explore the essential features that our AI-driven agent offers to guide you towards Canadian Permanent Residency.
          </p>
          
          <div className="features-container">
            <div className="feature-item">
              <h3>Personalized Score Assessment</h3>
              <p>Instantly understand your Comprehensive Ranking System (CRS) score and receive guidance on how to improve it.</p>
            </div>

            <div className="feature-item">
              <h3>Program & Stream Analysis</h3>
              <p>Discover the federal and provincial programs best suited to your background, from Federal Skilled Worker to Provincial Nominee Programs and more.</p>
            </div>

            <div className="feature-item">
              <h3>Detailed Next Steps & Recommendations</h3>
              <p>Receive targeted suggestions to enhance language scores, gain relevant work experience, or refine educational credentials all curated by our AI.</p>
            </div>
          </div>

          <div className="features-button-container">
            <button className="btn-primary">
              Learn More
            </button>
          </div>
        </section>

        {/* Unlock Your Path Section */}
        <section className="unlock-section">
          <div className="unlock-container-top">
            <h1>Unlock Your Path to Canadian Permanent Residency</h1>
          </div>

          <div className="unlock-big-block">
            {/* Left Side: Text Blocks */}
            <div className="unlock-small-blocks">
              <div className="unlock-text-block">
                <h3>Tailored Programs</h3>
                <p>Discover personalized immigration pathways designed for your unique scores, goals, and eligibility. We analyze top federal and provincial programs, ensuring the best options for you.</p>
              </div>
              <span className="down-arrow">↓</span>
              <div className="unlock-text-block">
                <h3>Step-by-Step Guidance</h3>
                <p>Follow detailed steps to boost your CRS scores, improve language proficiency, upgrade education, and meet work experience requirements to align with immigration program cutoffs.</p>
              </div>
              <span className="down-arrow">↓</span>
              <div className="unlock-text-block">
                <h3>Clear Outcomes</h3>
                <p>Understand how each step contributes to your success. Gain predictive insights on how to exceed program thresholds and secure your Invitation to Apply (ITA).</p>
              </div>

              <div className="unlock-button-container">
                <button className="btn-primary">
                  See my Pathways
                </button>
              </div>
            </div>

            {/* Right Side: Image */}
            <div className="unlock-image-block">
              <img src="/Images/Pathways.png" alt="Unlock Your Path" />
            </div>
          </div>
        </section>

        {/* How It Works Section */}
        <section className="how-it-works">
          <h2 className="how-it-works-title">How It Works</h2>
          <p className="how-it-works-subtitle">
            Follow these simple steps to get started and receive personalized guidance all at no cost.
          </p>

          <div className="steps-container">
            {/* Step 1 */}
            <div className="step">
              <img src="/Images/Icons/1.png" alt="Step 1" className="step-number" />
              <h3>Sign Up</h3>
              <p>Create your free account in just a few clicks. It's quick, easy, and gives you immediate access to our powerful AI Immigration Agent.</p>
            </div>

            {/* Arrow Image */}
            <div className="step-arrow">
              <img src="/Images/Icons/Arrow.png" alt="Arrow Down" />
            </div>

            {/* Step 2 */}
            <div className="step">
              <img src="/Images/Icons/2.png" alt="Step 2" className="step-number" />
              <h3>Fill Out the Profile Form</h3>
              <p>Provide your details such as work experience, education, language scores, and other key information in our secure and user-friendly form.</p>
            </div>

            {/* Arrow Image */}
            <div className="step-arrow">
              <img src="/Images/Icons/Arrow.png" alt="Arrow Down" />
            </div>

            {/* Step 3 */}
            <div className="step">
              <img src="/Images/Icons/3.png" alt="Step 3" className="step-number" />
              <h3>Get Your Personalized Immigration Roadmap</h3>
              <p>Receive a detailed breakdown of your CRS and BC PNP scores along with an evaluation of the Canadian immigration programs you're closest to qualifying for.</p>
            </div>
          </div>
        </section>

        {/* Meet Your Mate Section */}
        <section className="meet-mate">
          <div className="mate-container">
            {/* Left Side: Image */}
            <div className="mate-image">
              <img src="/Images/business-image.png" alt="AI Immigration Assistant" />
            </div>

            {/* Right Side: Text Content */}
            <div className="mate-text">
              <h2>Meet Your Personal AI Immigration Assistant</h2>
              <p>
                Our AI-powered Mate integrates seamlessly with federal and British Columbia 
                immigration programs, using your profile data to guide you through every 
                step of your Canadian immigration journey. From understanding your eligibility 
                to improving your scores, Mate is here to make your path to Permanent Residency 
                easier and stress-free.
              </p>
              <Link to="/chat" className="btn-primary">
                Chat with our Mate
              </Link>
            </div>
          </div>
        </section>

        {/* FAQ Section */}
        <section className="faq-section">
          <h2 className="faq-title">Frequently Asked Questions</h2>
          <p className="faq-subtitle">FAQ</p>

          <div className="faq-container">
            <FAQItem 
              question="How does the AI Immigration Agent determine my eligibility for Canadian Permanent Residency?"
              answer={<>
                <p>The AI Immigration Agent reviews various aspects of your profile, including:</p>
                <ul>
                  <li>Education credentials (and their Canadian equivalencies)</li>
                  <li>Work experience in skilled occupations</li>
                  <li>Language test scores (IELTS, CELPIP, or TEF)</li>
                  <li>Additional factors such as age, adaptability, and arranged employment</li>
                </ul>
                <p>By comparing these elements against established immigration criteria, it provides a clear assessment of your eligibility and highlights areas where you can improve.</p>
              </>}
            />
            
            <FAQItem 
              question="What steps can I take to improve my Comprehensive Ranking System (CRS) score?"
              answer={<>
                <ul>
                  <li>Increasing your language proficiency scores to achieve higher CLB levels</li>
                  <li>Obtaining further educational credentials or getting existing ones assessed</li>
                  <li>Gaining more skilled Canadian work experience</li>
                  <li>Pursuing provincial nominations or valid job offers to add bonus points</li>
                </ul>
                <p>The AI Immigration Agent tailors these recommendations to your unique situation, guiding you on which actions can yield the greatest benefits.</p>
              </>}
            />
            
            <FAQItem 
              question="How do I know which federal or provincial program is best suited to my profile?"
              answer={<p>The AI Immigration Agent identifies the programs and streams that best match your strengths, whether that's education, work experience, or language ability. It suggests federal programs (like Federal Skilled Worker) or provincial nominee streams that value your particular attributes, helping you focus on the most promising pathways to Canadian PR.</p>}
            />
            
            <FAQItem 
              question="Are the recommendations and action steps provided by the AI really free?"
              answer={<p>Yes, our AI Immigration Agent provides free eligibility assessments, recommendations, and insights. Some premium services may be available for additional features, but the core AI assessment is completely free.</p>}
            />
            
            <FAQItem 
              question="Can I ask follow-up questions or seek clarification after receiving my initial assessment?"
              answer={<p>Absolutely! You can engage with our AI Immigration Assistant for follow-up questions, additional clarifications, and tailored guidance based on your profile.</p>}
            />
          </div>
        </section>
      </main>
      
      <Footer />
    </>
  );
};

// FAQ Item Component
const FAQItem = ({ question, answer }) => {
  const [isOpen, setIsOpen] = React.useState(false);
  
  return (
    <div className={`faq-item ${isOpen ? 'active' : ''}`}>
      <button 
        className="faq-question"
        onClick={() => setIsOpen(!isOpen)}
      >
        {question}
        <span className="faq-icon">{isOpen ? '-' : '+'}</span>
      </button>
      <div className="faq-answer">
        {answer}
      </div>
    </div>
  );
};

export default HomePage; 